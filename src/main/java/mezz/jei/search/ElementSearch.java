package mezz.jei.search;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import mezz.jei.config.Config;
import mezz.jei.gui.ingredients.IIngredientListElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ElementSearch implements IElementSearch {
    
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<PrefixInfo, PrefixedSearchable> prefixedSearchables = new Reference2ObjectOpenHashMap<>();
    private final CombinedSearchables<IIngredientListElement<?>> combinedSearchables = new CombinedSearchables<>();

    public ElementSearch() {
        for (PrefixInfo prefixInfo : PrefixInfo.all()) {
            ISearchStorage<IIngredientListElement<?>> storage = prefixInfo.createStorage();
            PrefixedSearchable prefixedSearchable = new PrefixedSearchable(storage, prefixInfo);
            this.prefixedSearchables.put(prefixInfo, prefixedSearchable);
            this.combinedSearchables.addSearchable(prefixedSearchable);
        }
    }

    @Override
    public Set<IIngredientListElement<?>> getSearchResults(TokenInfo tokenInfo) {
        String token = tokenInfo.token;
        if (token.isEmpty()) {
            return Collections.emptySet();
        }
        Set<IIngredientListElement<?>> results = new ReferenceOpenHashSet<>();
        PrefixInfo prefixInfo = tokenInfo.prefixInfo;
        if (prefixInfo == PrefixInfo.NO_PREFIX) {
            combinedSearchables.getSearchResults(token, results);
            return results;
        }
        final ISearchable<IIngredientListElement<?>> searchable = this.prefixedSearchables.get(prefixInfo);
        if (searchable == null || searchable.getMode() == Config.SearchMode.DISABLED) {
            combinedSearchables.getSearchResults(token, results);
            return results;
        }
        searchable.getSearchResults(token, results);
        return results;
    }

    @Override
    public void add(IIngredientListElement<?> info) {
        for (PrefixedSearchable prefixedSearchable : this.prefixedSearchables.values()) {
            Config.SearchMode searchMode = prefixedSearchable.getMode();
            if (searchMode != Config.SearchMode.DISABLED) {
                Collection<String> strings = prefixedSearchable.getStrings(info);
                ISearchStorage<IIngredientListElement<?>> searchable = prefixedSearchable.getSearchStorage();
                for (String string : strings) {
                    searchable.put(string, info);
                }
            }
        }
    }

    @Override
    public Set<IIngredientListElement<?>> getAllIngredients() {
        Set<IIngredientListElement<?>> results = new ReferenceOpenHashSet<>();
        this.prefixedSearchables.get(PrefixInfo.NO_PREFIX).getAllElements(results);
        return results;
    }

    @Override
    public void logStatistics() {
        for (Map.Entry<PrefixInfo, PrefixedSearchable> entry : this.prefixedSearchables.entrySet()) {
            PrefixInfo prefixInfo = entry.getKey();
            if (prefixInfo.getMode() != Config.SearchMode.DISABLED) {
                ISearchStorage<IIngredientListElement<?>> storage = entry.getValue().getSearchStorage();
                LOGGER.info("ElementSearch {} Storage Stats: {}", prefixInfo, storage.statistics());
                try {
                    FileWriter fileWriter = new FileWriter("GeneralizedSuffixTree-" + prefixInfo + ".dot");
                    try (PrintWriter out = new PrintWriter(fileWriter)) {
                        storage.printTree(out, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}