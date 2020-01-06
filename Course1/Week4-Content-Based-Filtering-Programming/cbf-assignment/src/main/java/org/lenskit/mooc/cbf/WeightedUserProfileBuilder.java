package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();

        // TODO Normalize the user's ratings
        // TODO Build the user's weighted profile

        double sumRatings = 0.0;
        for (Rating rating : ratings) {
            sumRatings += rating.getValue();
        }
        double meanRating = sumRatings / ratings.size();

        for (Rating rating : ratings) {
            long itemId = rating.getItemId();
            Map<String, Double> itemVector = model.getItemVector(itemId);

            double preferenceWeight = rating.getValue() - meanRating;

            for (String term : itemVector.keySet()) {
                double val = 0.0;
                if(profile.containsKey(term))
                    val = profile.get(term) + preferenceWeight * itemVector.get(term);
                else
                    val = preferenceWeight * itemVector.get(term);
                profile.put(term, val);
            }
        }


        // The profile is accumulated, return it.
        return profile;
    }
}
