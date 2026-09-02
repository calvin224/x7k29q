package com.calvinpower.weatherservice.services.statistics;

import com.calvinpower.weatherservice.model.Statistic;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StatisticStrategyFactory {

    private final Map<Statistic, StatisticStrategy> strategies;

    public StatisticStrategyFactory(List<StatisticStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        StatisticStrategy::getStatistic,
                        Function.identity()
                ));
    }

    public StatisticStrategy getStrategy(Statistic statistic) {
        StatisticStrategy strategy = strategies.get(statistic);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported statistic: " + statistic
            );
        }

        return strategy;
    }
}