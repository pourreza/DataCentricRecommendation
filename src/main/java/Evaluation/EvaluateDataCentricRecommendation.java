package Evaluation;

import DataCentricRecommendation.DataCentricRecommendation;
import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import serviceWorkflowNetwork.*;

import java.io.*;
import java.util.*;

import static utilities.Printer.print;

public class EvaluateDataCentricRecommendation {
    public static final int NUMBER_OF_UNIQUE_DATES = 716;

    public static Graph<String, DefaultEdge>[] incompleteSimpleGraph = new Graph[NUMBER_OF_UNIQUE_DATES];
    public static Graph<String, DefaultEdge>[] completeSimpleGraph = new Graph[NUMBER_OF_UNIQUE_DATES];

    public static Graph<String, DefaultWeightedEdge>[] inclompleteGraph = new Graph[NUMBER_OF_UNIQUE_DATES]; // I call this incomplete because this does not have the local workers
    public static Graph<String, DefaultWeightedEdge>[] completeGraph = new Graph[NUMBER_OF_UNIQUE_DATES];  // I call this complete because in this graph we have added everything as first class citizens and we have more nodes and edges

    public static Set<SService>[] servicesInDate;
    public static Set<OOperation>[] operationsInDate;
    public static Set<SService>[] servicesWithLocalsInDate;
    public static Set<OOperation>[] operationsWithLocalsInDate;
    public static Set<WorkflowVersion>[] workflowsInDate;

    public static ArrayList<Date> uniqueSortedDates;

    public static void main(String... args) {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        for()
        DataCentricRecommendation recommendationAlgm = new DataCentricRecommendation(completeGraph, workflowsInDate[NUMBER_OF_UNIQUE_DATES-1])



//        int initialNodes = inclompleteGraph.vertexSet().size();
//        int newNodes = completeGraph.vertexSet().size();
//        int initialEdges = inclompleteGraph.edgeSet().size();
//        newEdges = completeGraph.edgeSet().size();
//
//        int directedWithoutLocalsPaths = numberOfPairsWithPath(incompleteSimpleGraph);
//
//        Graph<String, DefaultEdge> incompleteGraphWithReverseSimple = addReverseEdgesToGraph(incompleteSimpleGraph);
//        int undirectedWithoutLocals = numberOfPairsWithPath(incompleteGraphWithReverseSimple);
//
////        Graph<String, DefaultEdge> completeGraphWithReverseEdges = addReverseEdgesToGraph(completeSimpleGraph);
//        int undirectedWithLocals = numberOfPairsWithPath(completeSimpleGraph);
//
//
//        print("Service List size "+servicesInDate.size());
//        print("Initial Number of Nodes: " + initialNodes);
//        print("Number of Nodes with Local Workers: " + newNodes);
//        print("********************************************");
//        print("Initial Number of Edges: " + initialEdges);
//        print("Number of Edges with Local Workers: " + newEdges);
//        print("********************************************");
//        print("Number of connected nodes in initial directed graph: " + directedWithoutLocalsPaths);
//        print("Number of connected nodes in initial undirected graph: " + undirectedWithoutLocals);
//        print("Number of connected nodes in new graph with local workers and undirected: " + undirectedWithLocals);
//        print("********************************************");

    }

    private static Graph<String, DefaultEdge> addReverseEdgesToGraph(Graph<String, DefaultEdge> graph) {
        Graph<String, DefaultEdge> graphWithReverseEdges = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        ArrayList<Pair<String, String>> allReverseEdges = new ArrayList<Pair<String, String>>();
        Set<DefaultEdge> edges = graph.edgeSet();
        for (DefaultEdge edge : edges) {
            allReverseEdges.add(new Pair<String, String>(graph.getEdgeTarget(edge), graph.getEdgeSource(edge)));
        }

        for (Pair<String, String> reverseEdge : allReverseEdges) {
            graphWithReverseEdges.addVertex(reverseEdge.getFirst());
            graphWithReverseEdges.addVertex(reverseEdge.getSecond());
            graphWithReverseEdges.addEdge(reverseEdge.getFirst(), reverseEdge.getSecond());
            graphWithReverseEdges.addEdge(reverseEdge.getSecond(), reverseEdge.getFirst());
        }
        return graphWithReverseEdges;
    }

    private static int numberOfPairsWithPath(Graph<String, DefaultEdge> graph, int dateIndex) {
        int numberOfPairsWithPaths = 0;
        int count = 0;
        for (SService service1 : servicesInDate[dateIndex]) {
            for (SService service2 : servicesInDate[dateIndex]) {
                if (!service1.equals(service2)) {
                    if (graph.containsVertex(service1.getURL()) && graph.containsVertex(service2.getURL())) {
                        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(graph);
                        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(service1.getURL()).getPath(service2.getURL());
                        if (path != null) {
                            numberOfPairsWithPaths++;
                        }
                    }
                }
            }
        }
        return numberOfPairsWithPaths;
    }

    private static Map<String, SService> createServiceMap(Set<SService> services) {
        Map<String, SService> serviceMap = new HashMap<String, SService>();
        for (SService service : services) {
            serviceMap.put(service.getURL(), service);
        }
        return serviceMap;
    }

    private static void init() throws IOException, ClassNotFoundException {

        FileInputStream fin = new FileInputStream("unique-sorted-dates");
        ObjectInputStream ios = new ObjectInputStream(fin);
        uniqueSortedDates = (ArrayList<Date>) ios.readObject();

//        AnalyzeDataOptimized extractor = new AnalyzeDataOptimized();
//        incompleteSimpleGraph = extractor.getDirectedServiceGraph(false, uniqueSortedDates);
//        servicesInDate = extractor.getAllServices();
//        operationsInDate = extractor.getAllOperations();
//        servicesInDate = setServiceIntents(servicesInDate, operationsInDate);
//        completeSimpleGraph = extractor.getDirectedServiceGraph(true, uniqueSortedDates);
//        servicesWithLocalsInDate = extractor.getAllServices();
//        operationsWithLocalsInDate = extractor.getAllOperations();
//        servicesWithLocalsInDate = setServiceIntents(servicesWithLocalsInDate, operationsWithLocalsInDate);
//        workflowsInDate = extractor.getAllWorkflowVersions();
//
//        for (Set<WorkflowVersion> workflowVersions : workflowsInDate) {
//            for(WorkflowVersion workflowVersion: workflowVersions) {
//                workflowVersion.setIntent();
//            }
//        }
//
//
//        FileOutputStream fout = new FileOutputStream("incomplete-graph");
//        FileOutputStream fout2 = new FileOutputStream("complete-graph");
//        FileOutputStream fout3 = new FileOutputStream("service-list");
//        FileOutputStream fout4 = new FileOutputStream("service-list-with-locals");
//        FileOutputStream fout5 = new FileOutputStream("all-workflows");
//        FileOutputStream fout6 = new FileOutputStream("incomplete-directed-simple-graph");
//        FileOutputStream fout7 = new FileOutputStream("complete-directed-simple-graph");
//        FileOutputStream fout8 = new FileOutputStream("operations-list");
//        FileOutputStream fout9 = new FileOutputStream("operations-list-with-locals");
//        ObjectOutputStream oos = new ObjectOutputStream(fout);
//        ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
//        ObjectOutputStream oos3 = new ObjectOutputStream(fout3);
//        ObjectOutputStream oos4 = new ObjectOutputStream(fout4);
//        ObjectOutputStream oos5 = new ObjectOutputStream(fout5);
//        ObjectOutputStream oos6 = new ObjectOutputStream(fout6);
//        ObjectOutputStream oos7 = new ObjectOutputStream(fout7);
//        ObjectOutputStream oos8 = new ObjectOutputStream(fout8);
//        ObjectOutputStream oos9 = new ObjectOutputStream(fout9);
//        oos3.writeObject(servicesInDate);
//        oos4.writeObject(servicesWithLocalsInDate);
//        oos5.writeObject(workflowsInDate);
//        oos6.writeObject(incompleteSimpleGraph);
//        oos7.writeObject(completeSimpleGraph);
//        oos8.writeObject(operationsInDate);
//        oos9.writeObject(operationsWithLocalsInDate);
//
//        inclompleteGraph = createServiceServiceGraph(incompleteSimpleGraph, servicesInDate, workflowsInDate);
//        completeGraph = createServiceServiceGraph(completeSimpleGraph, servicesWithLocalsInDate, workflowsInDate);
//        oos.writeObject(inclompleteGraph);
//        oos2.writeObject(completeGraph);

        FileInputStream fout = new FileInputStream("incomplete-graph");
        FileInputStream fout2 = new FileInputStream("complete-graph");
        FileInputStream fout3 = new FileInputStream("service-list");
        FileInputStream fout4 = new FileInputStream("service-list-with-locals");
        FileInputStream fout5 = new FileInputStream("all-workflows");
        FileInputStream fout6 = new FileInputStream("incomplete-directed-simple-graph");
        FileInputStream fout7 = new FileInputStream("complete-directed-simple-graph");
        FileInputStream fout8 = new FileInputStream("operations-list");
        FileInputStream fout9 = new FileInputStream("operations-list-with-locals");
        ObjectInputStream oos = new ObjectInputStream(fout);
        ObjectInputStream oos2 = new ObjectInputStream(fout2);
        ObjectInputStream oos3 = new ObjectInputStream(fout3);
        ObjectInputStream oos4 = new ObjectInputStream(fout4);
        ObjectInputStream oos5 = new ObjectInputStream(fout5);
        ObjectInputStream oos6 = new ObjectInputStream(fout6);
        ObjectInputStream oos7 = new ObjectInputStream(fout7);
        ObjectInputStream oos8 = new ObjectInputStream(fout8);
        ObjectInputStream oos9 = new ObjectInputStream(fout9);
        inclompleteGraph = (Graph<String, DefaultWeightedEdge>[]) oos.readObject();
        completeGraph = (Graph<String, DefaultWeightedEdge>[]) oos2.readObject();
        servicesInDate = (Set<SService>[]) oos3.readObject();
        servicesWithLocalsInDate = (Set<SService>[]) oos4.readObject();
        workflowsInDate = (Set<WorkflowVersion>[]) oos5.readObject();
        incompleteSimpleGraph = (Graph<String, DefaultEdge>[]) oos6.readObject();
        completeSimpleGraph = (Graph<String, DefaultEdge>[]) oos7.readObject();
        operationsInDate = (Set<OOperation>[]) oos8.readObject();
        operationsWithLocalsInDate = (Set<OOperation>[]) oos9.readObject();

        print("services size: "+servicesInDate[715].size());
//
//        Set<Date> dates = new HashSet<Date>();
//        for (WorkflowVersion workflowVersion : workflowsInDate) {
//            dates.add(workflowVersion.getDate());
//        }
//        uniqueSortedDates = new ArrayList<Date>(dates);
//        Collections.sort(uniqueSortedDates);
//        print(uniqueSortedDates.size() + "size");
//
    }

    private static Set<SService>[] setServiceIntents(Set<SService>[] servicesTest1, Set<OOperation>[] operationsTest1) {
        Set<SService>[] servicesWithInents = new Set[NUMBER_OF_UNIQUE_DATES];
        for (int i = 0; i < servicesTest1.length; i++) {
            ArrayList<SService> serviceList = new ArrayList<SService>(servicesTest1[i]);
            servicesWithInents[i] = new HashSet<SService>();
            for (SService serivce : serviceList) {
                String intent = "";
                for (OOperation operation : operationsTest1[i]) {
                    if (operation.getService().equals(serivce)) {
                        intent += operation.getProcessorName() + " " + operation.getName() + " ";
                    }
                }
                serivce.setIntent(intent);
                servicesWithInents[i].add(serivce);
            }
        }
        return servicesWithInents;
    }

    private static Graph<String, DefaultWeightedEdge>[] createServiceServiceGraph(Graph<String, DefaultEdge>[] graph, Set<SService>[] services, Set<WorkflowVersion>[] workflowVersions) {
        Graph<String, DefaultWeightedEdge>[] weightedGraphs = new Graph[NUMBER_OF_UNIQUE_DATES];

        for (int i = 0; i < NUMBER_OF_UNIQUE_DATES; i++) {
            Map<String, SService> serviceMap = createServiceMap(services[i]);
            Graph<String, DefaultWeightedEdge> weightedGraph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
            int edgeNumber = 0;
            for (DefaultEdge edge : graph[i].edgeSet()) {
                print("Edge number:" + edgeNumber++);
                SService source = serviceMap.get(graph[i].getEdgeSource(edge));
                SService target = serviceMap.get(graph[i].getEdgeTarget(edge));
                weightedGraph.addVertex(source.getURL());
                weightedGraph.addVertex(target.getURL());
                if (source.equals(target)) {
                    print("We have looooooooops X_X");
                }
                if (edgeNumber == 25) {
                    print("HEEEEELP!!!!!");
                }

                Set<Integer> intersectedWorkflows = new HashSet<Integer>();
                ArrayList<WorkflowVersion> workflowVersionsList = new ArrayList<WorkflowVersion>(workflowVersions[i]);
                for (int k = 0; k < workflowVersionsList.size(); k++) {
                    ArrayList<OOperation> externalOperations = workflowVersionsList.get(k).getExternalOperations();
                    ArrayList<SService> servicesInWorkflow = new ArrayList<SService>();
                    for (int kk = 0; kk < externalOperations.size(); kk++) {
                        servicesInWorkflow.add(externalOperations.get(kk).getService());
                    }
                    if (servicesInWorkflow.contains(source) && servicesInWorkflow.contains(target))
                        intersectedWorkflows.add(workflowVersionsList.get(k).getWorkflow().getIndex());
                }
                if (intersectedWorkflows.size() > 0) {
                    DefaultWeightedEdge e1 = weightedGraph.addEdge(source.getURL(), target.getURL());
                    if (e1 != null) {
                        weightedGraph.setEdgeWeight(e1, intersectedWorkflows.size());
                    }
                }
            }
        }
        return weightedGraphs;
    }
}
