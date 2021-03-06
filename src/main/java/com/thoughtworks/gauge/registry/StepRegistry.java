/*----------------------------------------------------------------
 *  Copyright (c) ThoughtWorks, Inc.
 *  Licensed under the Apache License, Version 2.0
 *  See LICENSE.txt in the project root for license information.
 *----------------------------------------------------------------*/
package com.thoughtworks.gauge.registry;

import com.thoughtworks.gauge.Logger;
import com.thoughtworks.gauge.StepRegistryEntry;
import com.thoughtworks.gauge.StepValue;
import gauge.messages.Messages;
import gauge.messages.Spec;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class StepRegistry {
    private ConcurrentHashMap<String, CopyOnWriteArrayList<StepRegistryEntry>> registry;

    public StepRegistry() {
        registry = new ConcurrentHashMap<>();
    }

    public void addStepImplementation(StepValue stepValue, Method method, boolean isExternal) {
        String stepText = stepValue.getStepText();
        registry.putIfAbsent(stepText, new CopyOnWriteArrayList<>());
        registry.get(stepText).add(new StepRegistryEntry(stepValue, method, isExternal));
    }

    public void clear() {
        this.registry = new ConcurrentHashMap<>();
    }

    public List<String> keys() {
        return Collections.list(this.registry.keys());
    };

    public boolean contains(String stepTemplateText) {
        return registry.containsKey(stepTemplateText);
    }

    public StepRegistryEntry get(String stepTemplateText) {
        return getFirstEntry(stepTemplateText);
    }

    public StepRegistryEntry getForCurrentProject(String stepTemplateText, Method method) {
        return registry.get(stepTemplateText).stream()
                .filter(e -> {
                    String reflectedMethodName = method.getDeclaringClass().getName() + "." + method.getName();
                    Logger.debug("Comparing '" + e.getFullyQualifiedName() + "' and '"
                            + reflectedMethodName + "'");
                    return !e.getIsExternal() && e.getFullyQualifiedName().equals(reflectedMethodName);
                })
                .findFirst().orElse(null);
    }

    private StepRegistryEntry getFirstEntry(String stepTemplateText) {
        return registry.getOrDefault(stepTemplateText, new CopyOnWriteArrayList<>()).stream()
                .findFirst()
                .orElse(new StepRegistryEntry());
    }

    public List<String> getAllStepAnnotationTexts() {
        return registry.values().stream().flatMap(Collection::stream)
                .map(entry -> entry.getStepValue().getStepAnnotationText())
                .collect(toList());
    }

    String getStepAnnotationFor(String stepTemplateText) {
        return registry.values().stream().flatMap(Collection::stream).map(StepRegistryEntry::getStepValue)
                .filter(stepValue -> stepValue.getStepText().equals(stepTemplateText))
                .map(StepValue::getStepAnnotationText).findFirst().orElse("");
    }

    public void remove(String stepTemplateText) {
        registry.remove(stepTemplateText);
    }

    List<StepRegistryEntry> getAllEntries(String stepText) {
        return registry.get(stepText);
    }

    public void removeSteps(String fileName) {
        ConcurrentHashMap<String, CopyOnWriteArrayList<StepRegistryEntry>> newRegistry = new ConcurrentHashMap<>();
        for (String key : registry.keySet()) {
            CopyOnWriteArrayList<StepRegistryEntry> newEntryList = registry.get(key).stream()
                    .filter(entry -> entry.getFileName() != null && !entry.getFileName().equals(fileName)).collect(toCollection(CopyOnWriteArrayList::new));
            if (newEntryList.size() > 0) {
                newRegistry.put(key, newEntryList);
            }
        }
        registry = newRegistry;
    }

    public void addStep(StepValue stepValue, StepRegistryEntry entry) {
        String stepText = stepValue.getStepText();
        registry.putIfAbsent(stepText, new CopyOnWriteArrayList<>());
        registry.get(stepText).add(entry);
    }

    public boolean hasMultipleImplementations(String stepToValidate) {
        return getAllEntries(stepToValidate).size() > 1;
    }

    public List<Messages.StepPositionsResponse.StepPosition> getStepPositions(String filePath) {
        List<Messages.StepPositionsResponse.StepPosition> stepPositionsList = new ArrayList<>();

        for (Map.Entry<String, CopyOnWriteArrayList<StepRegistryEntry>> entryList : registry.entrySet()) {
            for (StepRegistryEntry entry : entryList.getValue()) {
                if (entry.getFileName().equals(filePath)) {
                    Messages.StepPositionsResponse.StepPosition stepPosition = Messages.StepPositionsResponse.StepPosition.newBuilder()
                            .setStepValue(entryList.getKey())
                            .setSpan(Spec.Span.newBuilder()
                                    .setStart(entry.getSpan().begin.line)
                                    .setStartChar(entry.getSpan().begin.column)
                                    .setEnd(entry.getSpan().end.line)
                                    .setEndChar(entry.getSpan().end.column).build())
                            .build();
                    stepPositionsList.add(stepPosition);
                }
            }
        }

        return stepPositionsList;
    }

    public boolean isFileCached(String fileName) {
        for (String key : registry.keySet()) {
            if (registry.get(key).stream().anyMatch(entry -> entry.getFileName().equals(fileName))) {
                return true;
            }
        }
        return false;
    }

    public String getFileName(String oldStepText) {
        Method method = get(oldStepText).getMethodInfo();
        if (method == null) {
            return "";
        }
        return method.getDeclaringClass().getCanonicalName().replace(".", File.separator) + ".java";
    }
}
