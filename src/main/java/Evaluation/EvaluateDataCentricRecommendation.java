package Evaluation;

import DataCentricRecommendation.CandidateScore;
import DataCentricRecommendation.DataCentricRecommendation;
import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import scala.tools.cmd.gen.AnyVals;
import serviceWorkflowNetwork.*;
import utilities.Printer;

import java.io.*;
import java.util.*;

import static utilities.Printer.print;
import static utilities.Printer.writeToFile;

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
    public static int[] canBePredicted;
    public static ArrayList<Pair<String, String>>[] newEdgesSources;
    public static ArrayList<Pair<String, String>>[] potentialOnes;

    public static Map<String, Integer> vertexIds = new HashMap<String, Integer>();
    private static ArrayList<ArrayList<Double>> nodeVector = new ArrayList<ArrayList<Double>>();
    private static ArrayList<Double> edgeVector;

    public static void main(String... args) throws IOException {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

//        findRecalls();
//        prepareTransEInputs();

        readTransEResutls();
//
        testTransE();
//        for(double intentWeigth = 0.3; intentWeigth<=0.8; intentWeigth+=0.1){
//            for(double contextWeight = 0.3; contextWeight<(1-intentWeigth); contextWeight+=0.1){
//                double pathLengthWeight = 1-intentWeigth - contextWeight;
//                print("Test for these weights: " + intentWeigth + "-" + contextWeight + "-" + pathLengthWeight);
//                calculatePrecisions(intentWeigth, contextWeight, pathLengthWeight);
//            }
//        }
//        initialDatasetEvaluation();
    }

    private static void readTransEResutls() throws IOException {
        int nodeId = 0;
        Graph<String, DefaultEdge> graph = incompleteSimpleGraph[NUMBER_OF_UNIQUE_DATES-1];
        for (String node : graph.vertexSet()) {
            vertexIds.put(node, nodeId);
            nodeId++;
        }

        File file = new File("entity2vec.bern");
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            nodeVector.add(new ArrayList<Double>());
            String[] doubleValues = line.split("\t");
            for(String str: doubleValues){
                nodeVector.get(nodeVector.size()-1).add(Double.parseDouble(str));
            }
        }

        File file2 = new File("relation2vec.bern");
        FileReader fileReader2 = new FileReader(file2);
        BufferedReader bufferedReader2 = new BufferedReader(fileReader2);
        String line2;
        edgeVector = new ArrayList<Double>();
        while ((line2 = bufferedReader2.readLine()) != null) {
            String[] doubleValues = line2.split("\t");
            for(String str: doubleValues){
                edgeVector.add(Double.parseDouble(str));
            }
        }

    }

    private static void testTransE() {

        Graph<String, DefaultEdge> testGraph = incompleteSimpleGraph[NUMBER_OF_UNIQUE_DATES - 1];
        ArrayList<Double> distances = new ArrayList<Double>();
        ArrayList<Integer> ranks = new ArrayList<Integer>();

        for (DefaultEdge edge : testGraph.edgeSet()) {
            String edgeSource = testGraph.getEdgeSource(edge);
            String edgeTarget = testGraph.getEdgeTarget(edge);
            ArrayList<Double> oneLevelAddition = addVectors(nodeVector.get(vertexIds.get(edgeSource)), edgeVector);
            ranks.add(recommend(oneLevelAddition, edgeSource, edgeTarget));
            distances.add(calculateDistance(oneLevelAddition, nodeVector.get(vertexIds.get(edgeTarget))));
        }

        Printer.writeToFile(distances, "transEDistancesRanksResults400.xlsx", ranks);

    }

    private static Integer recommend(ArrayList<Double> oneLevelAddition, String edgeSource, String edgeTarget) {
        Set<CandidateScore> candidateScores = new HashSet<CandidateScore>();
        for(SService service: servicesInDate[NUMBER_OF_UNIQUE_DATES-1]){
            if(!service.getURL().equals(edgeSource)){
                Pair<String, String> pair = new Pair<String, String>(edgeSource, service.getURL());
                Double score = calculateDistance(oneLevelAddition, nodeVector.get(vertexIds.get(service.getURL())));
                CandidateScore candidateScore = new CandidateScore(pair, score);
                candidateScores.add(candidateScore);
            }
        }
        ArrayList<CandidateScore> candidateScoreArrayList = new ArrayList<CandidateScore>(candidateScores);
        Collections.sort(candidateScoreArrayList);

        int rank = 0;
        for(int i=0; i<candidateScoreArrayList.size(); i++){
            if(candidateScoreArrayList.get(i).getCandidate().getFirst().equals(edgeSource) && candidateScoreArrayList.get(i).getCandidate().getSecond().equals(edgeTarget)){
                return (i+1);
            }
        }
        return candidateScoreArrayList.size()+1;
    }

    private static Double calculateDistance(ArrayList<Double> transEVector, ArrayList<Double> targetVector) {
        double distance = 0;
        for (int vectorIndex = 0; vectorIndex < targetVector.size(); vectorIndex++) {
            double dist = targetVector.get(vectorIndex) - transEVector.get(vectorIndex);
            distance += (dist * dist);
        }
        return Math.sqrt(distance);
    }

    private static ArrayList<Double> addVectors(ArrayList<Double> nodeVector, ArrayList<Double> edgeVector) {
        ArrayList<Double> addition = new ArrayList<Double>();
        for (int vectorIndex = 0; vectorIndex < nodeVector.size(); vectorIndex++) {
            addition.add(nodeVector.get(vectorIndex) + edgeVector.get(vectorIndex));
        }
        return addition;
    }

    private static void initialDatasetEvaluation() {
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

    private static void prepareTransEInputs() {
        print("I am in preparing TransE inputs");
        int countFile = 0;
        Map<String, Integer> paperIds = new HashMap<String, Integer>();
        int timeIndex = NUMBER_OF_UNIQUE_DATES - 1;
        countFile++;
        String train = "";
        Graph<String, DefaultEdge> graph = incompleteSimpleGraph[timeIndex];
        for (DefaultEdge edge : graph.edgeSet()) {
            String edgeSource = graph.getEdgeSource(edge);
            String edgeTarget = graph.getEdgeTarget(edge);
            train += (edgeSource + " " + edgeTarget + " FB \n");
        }
        int nodeId = 0;
        String entity2id = "";
        for (String node : graph.vertexSet()) {
            entity2id += (node + " " + nodeId + "\n");
            vertexIds.put(node, nodeId);
            nodeId++;
        }
        String trainFileName = "train.txt";
        print("Writing in file: " + trainFileName);
        Printer.writeToFile(train, trainFileName);
        String entityFileName = "entity2id.txt";
        print("Writing in file: " + entityFileName);
        Printer.writeToFile(entity2id, entityFileName);
    }

    private static void calculatePrecisions(double intentWeigth, double contextWeight, double pathLengthWeight) {
        print("In Precision Evaluation");

        ArrayList<Date> importantDates = new ArrayList<Date>();
        ArrayList<Long> time = new ArrayList<Long>();

        ArrayList<Integer> allNewOnes = new ArrayList<Integer>();
        ArrayList<Integer> potentialEdges = new ArrayList<Integer>();

        ArrayList<String> ranksInCandidateList = new ArrayList<String>();
        ArrayList<Integer> candidateListSize = new ArrayList<Integer>();
        ArrayList<Double> maxScores = new ArrayList<Double>();
        ArrayList<Double> minScores = new ArrayList<Double>();
        ArrayList<String> candidatesScores = new ArrayList<String>();

        for (int timeIndex = 0; timeIndex < NUMBER_OF_UNIQUE_DATES - 1; timeIndex++) {
            if (canBePredicted[timeIndex + 1] != 0) {
                importantDates.add(uniqueSortedDates.get(timeIndex + 1));
                allNewOnes.add(newEdgesSources[timeIndex + 1].size());
                potentialEdges.add(canBePredicted[timeIndex + 1]);

                print("Evaluating time = " + (timeIndex + 1));
                Map<String, SService> serviceMap = createServiceMap(servicesWithLocalsInDate[timeIndex], timeIndex);
                Graph<String, DefaultWeightedEdge> completeGraphWithReverseEdges = addReverseEdgesToWeightedGraph(completeGraph[timeIndex]);
                DataCentricRecommendation recommendation = new DataCentricRecommendation(completeGraphWithReverseEdges, workflowsInDate[timeIndex], servicesInDate[timeIndex], serviceMap, incompleteSimpleGraph[timeIndex], intentWeigth, contextWeight, pathLengthWeight);
                long beforeTime = System.currentTimeMillis();
                int recommededCandidates = recommendation.recommend(newEdgesSources[timeIndex + 1]);
                long endTime = System.currentTimeMillis();
                candidateListSize.add(recommededCandidates);
                ranksInCandidateList.add(recommendation.getRanks(potentialOnes[timeIndex + 1]));
                candidatesScores.add(recommendation.getScores(potentialOnes[timeIndex + 1]));
                minScores.add(recommendation.getMinScore());
                maxScores.add(recommendation.getMaxScore());
                time.add(endTime - beforeTime);
//                if (recommendedEdges != null) {
//                    totalReported.add(recommendedEdges.size());
//                    int correctOnes = 0;
//                    for (int recommedationIndex = 0; recommedationIndex < recommendedEdges.size(); recommedationIndex++) {
//                        if (incompleteSimpleGraph[timeIndex + 1].containsEdge(recommendedEdges.get(recommedationIndex).getFirst(), recommendedEdges.get(recommedationIndex).getSecond())) {
//                            correctOnes++;
//                        }
//                    }
//                    correctlyPredicted.add(correctOnes);
//                }
            }
        }
        //It should be noted that the time index that we get the results for are actually timeIndex+1
//        for(int timeIndex=0; timeIndex<totalReported.size(); timeIndex++){
//            if(totalReported.get(timeIndex)!=0){
//                precisions.add((double) correctlyPredicted.get(timeIndex)/totalReported.get(timeIndex));
//            }else{
//                precisions.add(0.);
//            }
//        }

        String fileIndex = "-" + intentWeigth + "-" + contextWeight + "-" + pathLengthWeight;

//        Printer.saveToExcel(importantDates, time, "Times"+fileIndex+".xlsx");
        Printer.saveToExcel(importantDates, allNewOnes, potentialEdges, candidateListSize, ranksInCandidateList, candidatesScores, minScores, maxScores, time, "All-Results" + fileIndex + ".xlsx");
    }

    private static void findRecalls() {

        print("In Recall Evaluation");

        newEdges = new int[NUMBER_OF_UNIQUE_DATES];
        newEdgesSources = new ArrayList[NUMBER_OF_UNIQUE_DATES];
        potentialOnes = new ArrayList[NUMBER_OF_UNIQUE_DATES];
        newEdgesSources[0] = new ArrayList<Pair<String, String>>();
        canBePredicted = new int[NUMBER_OF_UNIQUE_DATES];

        for (int timeIndex = 0; timeIndex < NUMBER_OF_UNIQUE_DATES - 1; timeIndex++) {
            print("Evaluating time = " + (timeIndex + 1));
            Graph<String, DefaultEdge> testGraph = incompleteSimpleGraph[timeIndex + 1];
            int countNewEdges = 0;
            newEdgesSources[timeIndex + 1] = new ArrayList<Pair<String, String>>();
            potentialOnes[timeIndex + 1] = new ArrayList<Pair<String, String>>();
            int couldBePredicted = 0;
            Map<String, SService> serviceMap = createServiceMap(servicesInDate[timeIndex], timeIndex);
            Graph<String, DefaultEdge> completeGraphWithReverseEdges = addReverseEdgesToGraph(completeSimpleGraph[timeIndex]);
            for (DefaultEdge edge : testGraph.edgeSet()) {
                String source = testGraph.getEdgeSource(edge);
                String target = testGraph.getEdgeTarget(edge);
                if (serviceMap.containsKey(source) && serviceMap.containsKey(target) && !incompleteSimpleGraph[timeIndex].containsEdge(source, target)) {
                    countNewEdges++;
                    newEdgesSources[timeIndex + 1].add(new Pair<String, String>(source, target));
                    if (completeGraphWithReverseEdges.containsVertex(source) && completeGraphWithReverseEdges.containsVertex(target)) {
                        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(completeGraphWithReverseEdges);
                        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(source).getPath(target);
                        if (path != null) {
                            couldBePredicted++;
                            potentialOnes[timeIndex + 1].add(new Pair<String, String>(source, target));
                        }
                    }
                }
            }
            newEdges[timeIndex + 1] = countNewEdges;
            canBePredicted[timeIndex + 1] = couldBePredicted;
        }

        //It should be noted that the time index that we get the results for are actually timeIndex+1
        double[] recalls = new double[NUMBER_OF_UNIQUE_DATES];
        for (int timeIndex = 1; timeIndex < NUMBER_OF_UNIQUE_DATES; timeIndex++) {
            if (newEdges[timeIndex] != 0) {
                recalls[timeIndex] = (double) canBePredicted[timeIndex] / newEdges[timeIndex];
            } else {
                recalls[timeIndex] = 0.;
                ;
            }
        }

        Printer.saveToExcel(uniqueSortedDates, newEdges, canBePredicted, recalls, "Recalls-v3.xlsx");
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
            if (defaultWeightedEdge != null) {
                graphWithReverseEdges.setEdgeWeight(defaultWeightedEdge, edgeWeight);
            }

            DefaultWeightedEdge defaultWeightedEdge1 = graphWithReverseEdges.addEdge(reverseEdge.getSecond(), reverseEdge.getFirst());
            if (defaultWeightedEdge1 != null) {
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
            if (service.getIntent() == null) {
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
        print("services size: " + servicesInDate[715].size());
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
        for (int i = 0; i < NUMBER_OF_UNIQUE_DATES; i++) {
            for (int j = i + 1; j < NUMBER_OF_UNIQUE_DATES; j++) {
                updateGraph(incompleteSimpleGraph[i], incompleteSimpleGraph[j]);
                updateGraph(completeSimpleGraph[i], completeSimpleGraph[j]);
                updateWeightedGraph(inclompleteGraph[i], inclompleteGraph[j]);
                updateWeightedGraph(completeGraph[i], completeGraph[j]);
            }
        }
    }

    private static void updateGraph(Graph<String, DefaultEdge> stringDefaultEdgeGraph, Graph<String, DefaultEdge> stringDefaultEdgeGraph1) {
        for (String vertex : stringDefaultEdgeGraph.vertexSet()) {
            stringDefaultEdgeGraph1.addVertex(vertex);
        }
        for (DefaultEdge edge : stringDefaultEdgeGraph.edgeSet()) {
            stringDefaultEdgeGraph1.addEdge(stringDefaultEdgeGraph.getEdgeSource(edge), stringDefaultEdgeGraph.getEdgeTarget(edge));
        }
    }

    private static void updateWeightedGraph(Graph<String, DefaultWeightedEdge> stringDefaultEdgeGraph, Graph<String, DefaultWeightedEdge> stringDefaultEdgeGraph1) {
        for (String vertex : stringDefaultEdgeGraph.vertexSet()) {
            stringDefaultEdgeGraph1.addVertex(vertex);
        }
        for (DefaultWeightedEdge edge : stringDefaultEdgeGraph.edgeSet()) {
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
