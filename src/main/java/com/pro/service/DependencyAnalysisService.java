package com.pro.service;

import com.pro.model.ComponentInfo;
import com.pro.model.ProjectAnalysis;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DependencyAnalysisService {

    /**
     * Enhances the project analysis with detailed dependency relationships
     */
    public void enhanceDependencyAnalysis(ProjectAnalysis analysis) {
        if (analysis == null)
            return;

        // Collect all components
        List<ComponentInfo> allComponents = new ArrayList<>();
        if (analysis.getControllers() != null)
            allComponents.addAll(analysis.getControllers());
        if (analysis.getServices() != null)
            allComponents.addAll(analysis.getServices());
        if (analysis.getRepositories() != null)
            allComponents.addAll(analysis.getRepositories());
        if (analysis.getModels() != null)
            allComponents.addAll(analysis.getModels());
        if (analysis.getConfigurations() != null)
            allComponents.addAll(analysis.getConfigurations());

        // Build enhanced dependency graph
        Map<String, List<String>> enhancedDependencyGraph = buildEnhancedDependencyGraph(allComponents);
        analysis.setDependencyGraph(enhancedDependencyGraph);

        // Update usedBy relationships
        updateUsedByRelationships(allComponents, enhancedDependencyGraph);
    }

    /**
     * Builds an enhanced dependency graph with better relationship mapping
     */
    private Map<String, List<String>> buildEnhancedDependencyGraph(List<ComponentInfo> allComponents) {
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        Map<String, ComponentInfo> componentMap = createComponentMap(allComponents);

        for (ComponentInfo component : allComponents) {
            String componentKey = component.getFullyQualifiedName();
            List<String> dependencies = new ArrayList<>();

            // Add direct dependencies (from @Autowired fields)
            if (component.getDependencies() != null) {
                for (String dependency : component.getDependencies()) {
                    String resolvedDependency = resolveDependency(dependency, componentMap);
                    if (resolvedDependency != null && !resolvedDependency.equals(componentKey)) {
                        dependencies.add(resolvedDependency);
                    }
                }
            }

            // Add method parameter dependencies (for constructor injection, etc.)
            dependencies.addAll(findMethodParameterDependencies(component, componentMap));

            // Remove duplicates and self-references
            dependencies = dependencies.stream()
                    .distinct()
                    .filter(dep -> !dep.equals(componentKey))
                    .collect(Collectors.toList());

            dependencyGraph.put(componentKey, dependencies);
        }

        return dependencyGraph;
    }

    /**
     * Creates a map of component names to ComponentInfo for quick lookup
     */
    private Map<String, ComponentInfo> createComponentMap(List<ComponentInfo> components) {
        Map<String, ComponentInfo> componentMap = new HashMap<>();

        for (ComponentInfo component : components) {
            // Add by fully qualified name
            componentMap.put(component.getFullyQualifiedName(), component);
            // Add by simple class name for easier lookup
            componentMap.put(component.getClassName(), component);
            // Add by class name with common variations
            componentMap.put(component.getClassName().toLowerCase(), component);
        }

        return componentMap;
    }

    /**
     * Resolves a dependency string to a fully qualified component name
     */
    private String resolveDependency(String dependency, Map<String, ComponentInfo> componentMap) {
        if (dependency == null || dependency.trim().isEmpty()) {
            return null;
        }

        // Remove generic type information and array brackets
        dependency = cleanDependencyString(dependency);

        // Try exact match first
        ComponentInfo component = componentMap.get(dependency);
        if (component != null) {
            return component.getFullyQualifiedName();
        }

        // Try simple name match
        component = componentMap.get(extractSimpleClassName(dependency));
        if (component != null) {
            return component.getFullyQualifiedName();
        }

        // Try partial matches for known Spring components
        for (Map.Entry<String, ComponentInfo> entry : componentMap.entrySet()) {
            ComponentInfo comp = entry.getValue();

            // Match by interface names or common patterns
            if (dependency.endsWith("Repository") && comp.getComponentType().equals("Repository")) {
                if (comp.getClassName().toLowerCase().contains(dependency.replace("Repository", "").toLowerCase())) {
                    return comp.getFullyQualifiedName();
                }
            }

            if (dependency.endsWith("Service") && comp.getComponentType().equals("Service")) {
                if (comp.getClassName().toLowerCase().contains(dependency.replace("Service", "").toLowerCase())) {
                    return comp.getFullyQualifiedName();
                }
            }
        }

        return null;
    }

    /**
     * Cleans dependency string by removing generics and array notation
     */
    private String cleanDependencyString(String dependency) {
        if (dependency == null)
            return null;

        // Remove generics like List<String>, Map<String, Object>
        dependency = dependency.replaceAll("<[^>]*>", "");

        // Remove array notation
        dependency = dependency.replace("[]", "");

        // Remove common prefixes/suffixes
        dependency = dependency.trim();

        return dependency;
    }

    /**
     * Extracts simple class name from fully qualified name
     */
    private String extractSimpleClassName(String fullyQualifiedName) {
        if (fullyQualifiedName == null)
            return null;

        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * Finds dependencies from method parameters (for constructor injection
     * analysis)
     */
    private List<String> findMethodParameterDependencies(ComponentInfo component,
            Map<String, ComponentInfo> componentMap) {
        List<String> parameterDependencies = new ArrayList<>();

        // This would need additional parsing to extract constructor parameters
        // For now, we'll return an empty list, but this could be enhanced
        // to parse constructor parameters from the JavaParser AST

        return parameterDependencies;
    }

    /**
     * Updates the usedBy relationships for all components
     */
    private void updateUsedByRelationships(List<ComponentInfo> allComponents,
            Map<String, List<String>> dependencyGraph) {
        // Initialize usedBy lists
        Map<String, List<String>> usedByMap = new HashMap<>();
        for (ComponentInfo component : allComponents) {
            usedByMap.put(component.getFullyQualifiedName(), new ArrayList<>());
        }

        // Build reverse relationships
        for (Map.Entry<String, List<String>> entry : dependencyGraph.entrySet()) {
            String source = entry.getKey();
            List<String> targets = entry.getValue();

            for (String target : targets) {
                List<String> usedByList = usedByMap.get(target);
                if (usedByList != null && !usedByList.contains(source)) {
                    usedByList.add(source);
                }
            }
        }

        // Update component usedBy lists
        for (ComponentInfo component : allComponents) {
            List<String> usedByList = usedByMap.get(component.getFullyQualifiedName());
            component.setUsedBy(usedByList != null ? usedByList : new ArrayList<>());
        }
    }

    /**
     * Calculates dependency metrics for the project
     */
    public Map<String, Object> calculateDependencyMetrics(ProjectAnalysis analysis) {
        Map<String, Object> metrics = new HashMap<>();

        if (analysis.getDependencyGraph() == null) {
            return metrics;
        }

        Map<String, List<String>> dependencyGraph = analysis.getDependencyGraph();

        // Calculate basic metrics
        int totalNodes = dependencyGraph.size();
        int totalEdges = dependencyGraph.values().stream()
                .mapToInt(List::size)
                .sum();

        // Calculate complexity metrics
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();

        for (String node : dependencyGraph.keySet()) {
            outDegree.put(node, dependencyGraph.get(node).size());
            inDegree.put(node, 0);
        }

        for (List<String> dependencies : dependencyGraph.values()) {
            for (String dependency : dependencies) {
                inDegree.put(dependency, inDegree.getOrDefault(dependency, 0) + 1);
            }
        }

        // Find highly connected components
        String mostDependedOn = inDegree.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        String mostDependent = outDegree.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        // Calculate average dependencies
        double avgDependencies = totalEdges / (double) totalNodes;

        metrics.put("totalNodes", totalNodes);
        metrics.put("totalEdges", totalEdges);
        metrics.put("averageDependencies", Math.round(avgDependencies * 100.0) / 100.0);
        metrics.put("mostDependedOnComponent", extractSimpleClassName(mostDependedOn));
        metrics.put("mostDependentComponent", extractSimpleClassName(mostDependent));
        metrics.put("maxInDegree", Collections.max(inDegree.values()));
        metrics.put("maxOutDegree", Collections.max(outDegree.values()));

        return metrics;
    }

    /**
     * Detects circular dependencies in the project
     */
    public List<List<String>> detectCircularDependencies(ProjectAnalysis analysis) {
        List<List<String>> cycles = new ArrayList<>();

        if (analysis.getDependencyGraph() == null) {
            return cycles;
        }

        Map<String, List<String>> graph = analysis.getDependencyGraph();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Map<String, String> parent = new HashMap<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                List<String> cycle = detectCycleFromNode(node, graph, visited, recursionStack, parent,
                        new ArrayList<>());
                if (!cycle.isEmpty()) {
                    cycles.add(cycle);
                }
            }
        }

        return cycles;
    }

    /**
     * DFS-based cycle detection from a specific node
     */
    private List<String> detectCycleFromNode(String node, Map<String, List<String>> graph,
            Set<String> visited, Set<String> recursionStack,
            Map<String, String> parent, List<String> currentPath) {

        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);

        List<String> neighbors = graph.getOrDefault(node, new ArrayList<>());

        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                parent.put(neighbor, node);
                List<String> cycle = detectCycleFromNode(neighbor, graph, visited, recursionStack, parent,
                        new ArrayList<>(currentPath));
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            } else if (recursionStack.contains(neighbor)) {
                // Cycle detected - build the cycle path
                List<String> cycle = new ArrayList<>();
                int startIndex = currentPath.indexOf(neighbor);
                if (startIndex >= 0) {
                    cycle.addAll(currentPath.subList(startIndex, currentPath.size()));
                    cycle.add(neighbor); // Complete the cycle
                }
                return cycle;
            }
        }

        recursionStack.remove(node);
        return new ArrayList<>();
    }
}