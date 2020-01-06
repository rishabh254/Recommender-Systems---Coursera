package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);

        // TODO Normalize the user's ratings by subtracting the item mean from each one.

        List<Result> results = new ArrayList<>();

        for (long candidateItem: items) {
            double candateItemMeanRating = itemMeans.get(candidateItem);

            double numerator = 0.0;
            double denominator = 0.0;

            Long2DoubleMap simNbr = this.model.getNeighbors(candidateItem);
            Long2DoubleMap simNbrRated = new Long2DoubleOpenHashMap();

            for (long neighbour: simNbr.keySet()) {
                if (ratings.containsKey(neighbour))
                    simNbrRated.put(neighbour, simNbr.get(neighbour));
            }

            // Take the top 20 most similar neighbours
            List<Double> top20ItemSimilarities = simNbrRated
                    .values().stream().sorted(Comparator.reverseOrder()).limit(this.neighborhoodSize).collect(Collectors.toList());

            for (long neighbour: simNbrRated.keySet()) {
                double similarity = simNbrRated.get(neighbour);

                if (top20ItemSimilarities.contains(similarity)) {
                    double userRating = ratings.get(neighbour);
                    double otherIteamMeanRating = itemMeans.get(neighbour);

                    numerator += (userRating - otherIteamMeanRating) * similarity;
                    denominator += Math.abs(similarity);
                }
            }

            if (denominator > 0.0) {
                double score = candateItemMeanRating + (numerator/denominator);
                results.add(Results.create(candidateItem, score));
            }
        }

        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }


}
