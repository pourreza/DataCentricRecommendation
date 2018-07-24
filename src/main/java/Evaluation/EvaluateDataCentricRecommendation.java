package Evaluation;

import DataCentricRecommendation.DataCentricRecommendation;
import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import scala.tools.nsc.doc.model.Def;
import serviceWorkflowNetwork.*;
import utilities.Printer;

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
    public static int[] newEdges;

    public static void main(String... args) {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        findRecalls();
        calculatePrecisions();


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
//        Graph<String, DefaultEdge> completeGraphWithReverseEdges = addReverseEdgesToGraph(completeSimpleGraph);
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

    private static void calculatePrecisions() {
        print("In Precision Evaluation");

        int[] totalReported = new int[NUMBER_OF_UNIQUE_DATES];
        int[] correctlyPredicted = new int[NUMBER_OF_UNIQUE_DATES];
        long[] time = new long[NUMBER_OF_UNIQUE_DATES];
        for(int timeIndex=0; timeIndex<NUMBER_OF_UNIQUE_DATES-1; timeIndex++){
            if(newEdges[timeIndex+1]!=0) {
                if (timeIndex == 34) {
                    print("HI");
                }
                print("Evaluating time = " + (timeIndex + 1));
                Map<String, SService> serviceMap = createServiceMap(servicesWithLocalsInDate[timeIndex], timeIndex);
                long beforeTime = System.currentTimeMillis();
                Graph<String, DefaultWeightedEdge> completeGraphWithReverseEdges = addReverseEdgesToWeightedGraph(completeGraph[timeIndex]);
                DataCentricRecommendation recommendation = new DataCentricRecommendation(completeGraphWithReverseEdges, workflowsInDate[timeIndex], servicesInDate[timeIndex], serviceMap);
                ArrayList<Pair<String, String>> recommendedEdges = recommendation.recommend();
                long endTime = System.currentTimeMillis();
                time[timeIndex] = endTime - beforeTime;
                if (recommendedEdges != null) {
                    totalReported[timeIndex + 1] = recommendedEdges.size();
                    int correctOnes = 0;
                    for (int recommedationIndex = 0; recommedationIndex < recommendedEdges.size(); recommedationIndex++) {
                        if (incompleteSimpleGraph[timeIndex + 1].containsEdge(recommendedEdges.get(recommedationIndex).getFirst(), recommendedEdges.get(recommedationIndex).getSecond())) {
                            correctOnes++;
                        }
                    }
                    correctlyPredicted[timeIndex + 1] = correctOnes;
                }
            }
        }
        //It should be noted that the time index that we get the results for are actually timeIndex+1
        double[] precisions = new double[NUMBER_OF_UNIQUE_DATES];
        for(int timeIndex=1; timeIndex<NUMBER_OF_UNIQUE_DATES; timeIndex++){
            if(totalReported[timeIndex]!=0){
                precisions[timeIndex] = (double) correctlyPredicted[timeIndex]/totalReported[timeIndex];
            }else{
                precisions[timeIndex] = 0.;
            }
        }

        Printer.saveToExcel(uniqueSortedDates, time, "Times-v0.xlsx");
        Printer.saveToExcel(uniqueSortedDates, totalReported, correctlyPredicted, precisions, "Precisions-v0.xlsx");
    }

    private static void findRecalls() {

        print("In Recall Evaluation");

        newEdges = new int[NUMBER_OF_UNIQUE_DATES];
        int[] canBePredicted = new int[NUMBER_OF_UNIQUE_DATES];
        for(int timeIndex=0; timeIndex< NUMBER_OF_UNIQUE_DATES-1; timeIndex++){
            print("Evaluating time = "+ (timeIndex+1));
            Graph<String, DefaultEdge> testGraph = incompleteSimpleGraph[timeIndex + 1];
            int countNewEdges = 0;
            int couldBePredicted = 0;
            Map<String, SService> serviceMap = createServiceMap(servicesInDate[timeIndex], timeIndex);
            Graph<String, DefaultEdge> completeGraphWithReverseEdges = addReverseEdgesToGraph(completeSimpleGraph[timeIndex]);
            for(DefaultEdge edge: testGraph.edgeSet()){
                String source = testGraph.getEdgeSource(edge);
                String target = testGraph.getEdgeTarget(edge);
                if(serviceMap.containsKey(source) && serviceMap.containsKey(target) && !incompleteSimpleGraph[timeIndex].containsEdge(source, target)){
                    countNewEdges++;
                    if(completeGraphWithReverseEdges.containsVertex(source) && completeGraphWithReverseEdges.containsVertex(target)) {
                        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(completeGraphWithReverseEdges);
                        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(source).getPath(target);
                        if (path != null) {
                            couldBePredicted++;
                        }
                    }
                }
            }
            newEdges[timeIndex+1] = countNewEdges;
            canBePredicted[timeIndex+1] = couldBePredicted;
        }

        //It should be noted that the time index that we get the results for are actually timeIndex+1
        double[] recalls = new double[NUMBER_OF_UNIQUE_DATES];
        for(int timeIndex=1; timeIndex<NUMBER_OF_UNIQUE_DATES; timeIndex++){
            if(newEdges[timeIndex]!=0){
                recalls[timeIndex] = (double) canBePredicted[timeIndex]/newEdges[timeIndex];
            }else{
                recalls[timeIndex] = 0.;;
            }
        }

        Printer.saveToExcel(uniqueSortedDates, newEdges, canBePredicted, recalls, "Recalls-v1.xlsx");
    }

    private static boolean containsSourceTargetServices(Set<SService> sServices, Graph<String, DefaultEdge> testGraph, DefaultEdge edge) {
        return sServices.contains(testGraph.getEdgeSource(edge)) && sServices.contains(testGraph.getEdgeTarget(edge));
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

    private static Graph<String, DefaultWeightedEdge> addReverseEdgesToWeightedGraph(Graph<String, DefaultWeightedEdge> graph) {
        Graph<String, DefaultWeightedEdge> graphWithReverseEdges = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        ArrayList<Pair<String, String>> allReverseEdges = new ArrayList<Pair<String, String>>();
        Set<DefaultWeightedEdge> edges = graph.edgeSet();
        for (DefaultWeightedEdge edge : edges) {
            allReverseEdges.add(new Pair<String, String>(graph.getEdgeTarget(edge), graph.getEdgeSource(edge)));
        }

        for (Pair<String, String> reverseEdge : allReverseEdges) {
            graphWithReverseEdges.addVertex(reverseEdge.getFirst());
            graphWithReverseEdges.addVertex(reverseEdge.getSecond());

            DefaultWeightedEdge weightedEdge = graph.getEdge(reverseEdge.getSecond(), reverseEdge.getFirst());
            double edgeWeight = graph.getEdgeWeight(weightedEdge);

            DefaultWeightedEdge defaultWeightedEdge = graphWithReverseEdges.addEdge(reverseEdge.getFirst(), reverseEdge.getSecond());
            if(defaultWeightedEdge!=null) {
                graphWithReverseEdges.setEdgeWeight(defaultWeightedEdge, edgeWeight);
            }

            DefaultWeightedEdge defaultWeightedEdge1 = graphWithReverseEdges.addEdge(reverseEdge.getSecond(), reverseEdge.getFirst());
            if(defaultWeightedEdge1!=null) {
                graphWithReverseEdges.setEdgeWeight(defaultWeightedEdge1, edgeWeight);
            }
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

    private static Map<String, SService> createServiceMap(Set<SService> services, int timeIndex) {
        Map<String, SService> serviceMap = new HashMap<String, SService>();
        for (SService service : services) {
            serviceMap.put(service.getURL(), service);
            if(service.getIntent()==null){
                service.setIntent(findIntent(service, timeIndex));
            }
        }
        return serviceMap;
    }

    private static String findIntent(SService service, int timeIndex) {
        String intent = "";
        for (OOperation operation : operationsWithLocalsInDate[timeIndex]) {
            if (operation.getService().equals(service)) {
                intent += operation.getProcessorName() + " " + operation.getName() + " ";
            }
        }
        return intent;
    }

    private static void init() throws IOException, ClassNotFoundException {

        FileInputStream fin0 = new FileInputStream("unique-sorted-dates");
        ObjectInputStream ios = new ObjectInputStream(fin0);
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
//

        FileInputStream fin = new FileInputStream("incomplete-graph");
        FileInputStream fin2 = new FileInputStream("complete-graph");
        FileInputStream fin3 = new FileInputStream("service-list");
        FileInputStream fin4 = new FileInputStream("service-list-with-locals");
        FileInputStream fin5 = new FileInputStream("all-workflows");
        FileInputStream fin6 = new FileInputStream("incomplete-directed-simple-graph");
        FileInputStream fin7 = new FileInputStream("complete-directed-simple-graph");
        FileInputStream fin8 = new FileInputStream("operations-list");
        FileInputStream fin9 = new FileInputStream("operations-list-with-locals");
        ObjectInputStream ois = new ObjectInputStream(fin);
        ObjectInputStream ois2 = new ObjectInputStream(fin2);
        ObjectInputStream ois3 = new ObjectInputStream(fin3);
        ObjectInputStream ois4 = new ObjectInputStream(fin4);
        ObjectInputStream ois5 = new ObjectInputStream(fin5);
        ObjectInputStream ois6 = new ObjectInputStream(fin6);
        ObjectInputStream ois7 = new ObjectInputStream(fin7);
        ObjectInputStream ois8 = new ObjectInputStream(fin8);
        ObjectInputStream ois9 = new ObjectInputStream(fin9);
        inclompleteGraph = (Graph<String, DefaultWeightedEdge>[]) ois.readObject();
        completeGraph = (Graph<String, DefaultWeightedEdge>[]) ois2.readObject();
        servicesInDate = (Set<SService>[]) ois3.readObject();
        servicesWithLocalsInDate = (Set<SService>[]) ois4.readObject();
        workflowsInDate = (Set<WorkflowVersion>[]) ois5.readObject();
        incompleteSimpleGraph = (Graph<String, DefaultEdge>[]) ois6.readObject();
        completeSimpleGraph = (Graph<String, DefaultEdge>[]) ois7.readObject();
        operationsInDate = (Set<OOperation>[]) ois8.readObject();
        operationsWithLocalsInDate = (Set<OOperation>[]) ois9.readObject();

//        inclompleteGraph = createServiceServiceGraph(incompleteSimpleGraph, servicesInDate, workflowsInDate);
//        completeGraph = createServiceServiceGraph(completeSimpleGraph, servicesWithLocalsInDate, workflowsInDate);
//
//        accumulateGraphs();
//
//        FileOutputStream fout = new FileOutputStream("incomplete-graph");
//        FileOutputStream fout2 = new FileOutputStream("complete-graph");
////        FileOutputStream fout3 = new FileOutputStream("service-list");
////        FileOutputStream fout4 = new FileOutputStream("service-list-with-locals");
////        FileOutputStream fout5 = new FileOutputStream("all-workflows");
//        FileOutputStream fout6 = new FileOutputStream("incomplete-directed-simple-graph");
//        FileOutputStream fout7 = new FileOutputStream("complete-directed-simple-graph");
////        FileOutputStream fout8 = new FileOutputStream("operations-list");
////        FileOutputStream fout9 = new FileOutputStream("operations-list-with-locals");
//        ObjectOutputStream oos = new ObjectOutputStream(fout);
//        ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
////        ObjectOutputStream oos3 = new ObjectOutputStream(fout3);
////        ObjectOutputStream oos4 = new ObjectOutputStream(fout4);
////        ObjectOutputStream oos5 = new ObjectOutputStream(fout5);
//        ObjectOutputStream oos6 = new ObjectOutputStream(fout6);
//        ObjectOutputStream oos7 = new ObjectOutputStream(fout7);
////        ObjectOutputStream oos8 = new ObjectOutputStream(fout8);
////        ObjectOutputStream oos9 = new ObjectOutputStream(fout9);
////        oos3.writeObject(servicesInDate);
////        oos4.writeObject(servicesWithLocalsInDate);
////        oos5.writeObject(workflowsInDate);
////        oos8.writeObject(operationsInDate);
//        oos9.writeObject(operationsWithLocalsInDate);
        print("services size: "+servicesInDate[715].size());
//
//        oos6.writeObject(incompleteSimpleGraph);
//        oos7.writeObject(completeSimpleGraph);
//        oos.writeObject(inclompleteGraph);
//        oos2.writeObject(completeGraph);
//        Set<Date> dates = new HashSet<Date>();
//        for (WorkflowVersion workflowVersion : workflowsInDate) {
//            dates.add(workflowVersion.getDate());
//        }
//        uniqueSortedDates = new ArrayList<Date>(dates);
//        Collections.sort(uniqueSortedDates);
//        print(uniqueSortedDates.size() + "size");
//
    }

    private static void accumulateGraphs() {
        for(int i=0; i<NUMBER_OF_UNIQUE_DATES; i++){
            for(int j=i+1; j<NUMBER_OF_UNIQUE_DATES; j++){
                updateGraph(incompleteSimpleGraph[i], incompleteSimpleGraph[j]);
                updateGraph(completeSimpleGraph[i], completeSimpleGraph[j]);
                updateWeightedGraph(inclompleteGraph[i], inclompleteGraph[j]);
                updateWeightedGraph(completeGraph[i], completeGraph[j]);
            }
        }
    }

    private static void updateGraph(Graph<String, DefaultEdge> stringDefaultEdgeGraph, Graph<String, DefaultEdge> stringDefaultEdgeGraph1) {
        for(String vertex: stringDefaultEdgeGraph.vertexSet()){
            stringDefaultEdgeGraph1.addVertex(vertex);
        }
        for(DefaultEdge edge: stringDefaultEdgeGraph.edgeSet()){
            stringDefaultEdgeGraph1.addEdge(stringDefaultEdgeGraph.getEdgeSource(edge), stringDefaultEdgeGraph.getEdgeTarget(edge));
        }
    }

    private static void updateWeightedGraph(Graph<String, DefaultWeightedEdge> stringDefaultEdgeGraph, Graph<String, DefaultWeightedEdge> stringDefaultEdgeGraph1) {
        for(String vertex: stringDefaultEdgeGraph.vertexSet()){
            stringDefaultEdgeGraph1.addVertex(vertex);
        }
        for(DefaultWeightedEdge edge: stringDefaultEdgeGraph.edgeSet()){
            stringDefaultEdgeGraph1.addEdge(stringDefaultEdgeGraph.getEdgeSource(edge), stringDefaultEdgeGraph.getEdgeTarget(edge));
        }
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
            Map<String, SService> serviceMap = createServiceMap(services[i], i);
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
            weightedGraphs[i] = weightedGraph;
        }
        return weightedGraphs;
    }
}
