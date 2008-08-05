/**
 * Preprocessor options:
 * #define LOG10 causes log10 probs, else logE probs will be used.
 */

#include <math.h>
#include <assert.h>

typedef double log_prob_t;
typedef double prob_t;
typedef int count_t;

/**
 * lm_log
 * @prob: a probability 0.0 <= p <= 1.0
 *
 * Convert a normal probability to a log probability of either base 10
 * or base e, depending on the preprocessor define LOG10.
 *
 * Return value: A log-domain probability.
 **/
static inline log_prob_t lm_log(prob_t prob) {
#ifdef LOG10
	return log10(prob);
#else
	return log(prob);
#endif
}

/**
 * lm_maximum_likelihood
 * @occurances: The number of times this event (e.g. n-gram) has occurred.
 * @event_count_sum: The sum of event counts (e.g. n-gram occurances) for this model
 * 						(e.g. the 4-gram model).
 *
 * Calculates the log-domain maximum likelihood estimate of an event.
 *
 * Return value: The log-domain maximum likelihood estimate of the specified event.
 **/
log_prob_t lm_maximum_liklihood(count_t occurances, count_t event_count_sum) {
	prob_t prob = (prob_t) occurances / (prob_t) event_count_sum;
	log_prob_t log_prob = lm_log(prob);
	return log_prob;
}

/**
 * lm_additive_smooth
 * @occurances: The observed number of times this event (e.g. n-gram) occurred.
 * @event_count_sum: The observed number of events (e.g. n-grams) for this model
 * 						(e.g. the 4-gram model).
 * @num_events: The observed number of events (e.g. 4-grams) in this model
 * @delta: A real valued count to be added to the observed counts of each event. 
 *
 * Calculates the log-domain estimate of an event using additive smoothing.
 *
 * Return value: The log-domain additive-smoothed estimate of the specified event.
 **/
log_prob_t lm_additive_smooth(count_t occurances, count_t event_count_sum,
		count_t num_events, prob_t delta) {

	prob_t numerator = (delta + (prob_t) occurances);
	prob_t denominator = (delta * num_events) + (prob_t) event_count_sum;
	assert(denominator > 0);
	prob_t prob = numerator / denominator;
	log_prob_t log_prob = lm_log(prob);
	return log_prob;
}

/**
 * lm_mod_knesser_ney_ngram
 * @occurances: The observed number of times this event (e.g. n-gram) occurred.
 * @event_count_sum: The observed number of events (e.g. n-grams) for this model
 * 						(e.g. the 4-gram model).
 * @num_events: The observed number of events (e.g. 4-grams) in this model
 * @delta: A real valued count to be added to the observed counts of each event. 
 *
 * Calculates the log-domain modified Knesser-Ney estimate of a SINGLE n-gram
 * component. Thus, to get the full modified Knesser-Ney estimate for n > 1,
 * This method must be combined with a call to lm_mod_knesser_ney_interpolate().
 *
 * Return value: The log-domain additive-smoothed estimate of the specified event.
 **/
log_prob_t lm_mod_knesser_ney_ngram(count_t occurances, count_t event_count_sum,
		count_t num_events, prob_t delta) {

	prob_t numerator = (prob_t) occurances - delta;
	if(numerator < 0.0)
		numerator = 0.0;
	numerator += delta;
	//numerator *= N1; // TODO: XXX
	
	// TODO: How do we generalize MKN to using many n-gram models at a time?

	assert(event_count_sum > 0);
	prob_t prob = numerator / event_count_sum;
	log_prob_t log_prob = lm_log(prob);
	return log_prob;
}

log_prob_t lm_mod_knesser_ney_interpolate(count_t occurances, count_t event_count_sum,
		count_t num_events, prob_t delta) {

}