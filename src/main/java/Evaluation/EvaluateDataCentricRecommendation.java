package Evaluation;

import DataCentricRecommendation.DataCentricRecommendation;
import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import serviceWorkflowNetwork.*;

import java.io.*;
import java.util.*;

import static utilities.Printer.print;

public class EvaluateDataCentricRecommendation {

    public static Graph<String, DefaultEdge> incompleteSimpleGraph;
    public static Graph<String, DefaultEdge> completeSimpleGraph;

    public static Graph<String, DefaultWeightedEdge> inclompleteGraph; // I call this incomplete because this does not have the local workers
    public static Graph<String, DefaultWeightedEdge> completeGraph;  // I call this complete because in this graph we have added everything as first class citizens and we have more nodes and edges

    public static Set<SService> serviceList;
    public static Set<SService> serviceListWithLocals;
    public static Set<WorkflowVersion> allWorkflowVersions;
    public static Map<Pair<String, String>, Double> initialServicesIncidentMatrix;
    public static Map<Pair<String, String>, Double> newServicesIncidentMatrix;

    public static void main(String... args) {
        init();

        int initialConnectedNodes = 0;
        int newConnectedNodes = 0;
        int initialNodes = 0;
        int newNodes = 0;
        int initialEdges = 0;
        int newEdges = 0;

        initialNodes = inclompleteGraph.vertexSet().size();
        newNodes = completeGraph.vertexSet().size();
        initialEdges = inclompleteGraph.edgeSet().size();
        newEdges = completeGraph.edgeSet().size();
        initialServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
        newServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();

        //QUESTION: should I also get the values for the initial graph ?

        Map<String, SService> serviceMap = createServiceMap(serviceListWithLocals);
        DataCentricRecommendation recommendationAlgorithm = new DataCentricRecommendation(completeGraph, allWorkflowVersions, serviceMap);
        recommendationAlgorithm.addAllReverseEdges();

        for (SService service1 : serviceList) {
            for (SService service2 : serviceList) {
                if (!service1.equals(service2)) {
                    if(inclompleteGraph.containsVertex(service1.getURL()) && inclompleteGraph.containsVertex(service2.getURL())) {
                        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(incompleteSimpleGraph);
                        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(service1.getURL()).getPath(service2.getURL());
                        double score = 0;
                        if(path!=null){
                            score = 1;
                        }
//                        double score = recommendationAlgorithm.getScore(service1, service2);
                        initialServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), score);
                    }else{
                        initialServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), -1.);
                    }
                }
            }
        }

        for (SService service1 : serviceListWithLocals) {
            for (SService service2 : serviceListWithLocals) {
                if (!service1.equals(service2)) {
                    if(completeGraph.containsVertex(service1.getURL()) && completeGraph.containsVertex(service2.getURL())) {
                        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(completeSimpleGraph);
                        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(service1.getURL()).getPath(service2.getURL());
                        double score = 0;
                        if(path!=null){
                            score = 1;
                        }
//                        double score = recommendationAlgorithm.getScore(service1, service2);
                        newServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), score);
                    }else{
                        newServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), -1.);
                    }
                }
            }
        }


        for (SService service1 : serviceList) {
            for (SService service2 : serviceList) {
                if (!service1.equals(service2)) {
                    Pair<String, String> pair = new Pair<String, String>(service1.getURL(), service2.getURL());
                    if(initialServicesIncidentMatrix.containsKey(pair)){
                        if(initialServicesIncidentMatrix.get(pair)<=0 && newServicesIncidentMatrix.get(pair)>0){
                            newConnectedNodes++;
                        }
                    }
//                    if(inclompleteGraph.containsVertex(service1.getURL()) && inclompleteGraph.containsVertex(service2.getURL())) {
//                        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(incompleteSimpleGraph);
//                        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(service1.getURL()).getPath(service2.getURL());
//                        double score = 0;
//                        if(path!=null){
//                            score = 1;
//                        }
////                        double score = recommendationAlgorithm.getScore(service1, service2);
//                        initialServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), score);
//                    }else{
//                        initialServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), -1.);
//                    }
                }
            }
        }

//        for (Pair<String, String> servicePair : newServicesIncidentMatrix.keySet()) {
//            if (newServicesIncidentMatrix.get(servicePair) > 0)
//                newConnectedNodes++;
//        }

        print("Initial Number of Nodes: " + initialNodes);
        print("Number of Nodes with Local Workers: " + newNodes);
        print("********************************************");
        print("Initial Number of Edges: " + initialEdges);
        print("Number of Edges with Local Workers: " + newEdges);
        print("********************************************");
        print("Number of connected nodes in initial graph: " + initialConnectedNodes);
        print("Number of connected nodes in new graph with local workers: " + newConnectedNodes);
        print("********************************************");

    }

    private static Map<String, SService> createServiceMap(Set<SService> services) {
        Map<String, SService> serviceMap = new HashMap<String, SService>();
        for (SService service : services) {
            serviceMap.put(service.getURL(), service);
        }
        return serviceMap;
    }

    private static void init() {
//        try {
//
//            AnalyzeDataOptimized extractor = new AnalyzeDataOptimized();
//            incompleteSimpleGraph = extractor.getDirectedServiceGraph(false);
//            serviceList = extractor.getAllServices();
//            completeSimpleGraph = extractor.getDirectedServiceGraph(true);
//            serviceListWithLocals = extractor.getAllServices();
//            allWorkflowVersions = extractor.getAllWorkflowVersions();
//            for (SService service : serviceListWithLocals) {
//                if (service.getType().equals(ServiceType.LOCAL))
//                    print(service.getURL());
//            }
//
//
//            FileOutputStream fout = new FileOutputStream("incomplete-graph");
//            FileOutputStream fout2 = new FileOutputStream("complete-graph");
//            FileOutputStream fout3 = new FileOutputStream("service-list");
//            FileOutputStream fout4 = new FileOutputStream("service-list-with-locals");
//            FileOutputStream fout5 = new FileOutputStream("all-workflows");
//            FileOutputStream fout6 = new FileOutputStream("incomplete-directed-simple-graph");
//            FileOutputStream fout7 = new FileOutputStream("complete-directed-simple-graph");
//            ObjectOutputStream oos = new ObjectOutputStream(fout);
//            ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
//            ObjectOutputStream oos3 = new ObjectOutputStream(fout3);
//            ObjectOutputStream oos4 = new ObjectOutputStream(fout4);
//            ObjectOutputStream oos5 = new ObjectOutputStream(fout5);
//            ObjectOutputStream oos6 = new ObjectOutputStream(fout6);
//            ObjectOutputStream oos7 = new ObjectOutputStream(fout7);
//            oos3.writeObject(serviceList);
//            oos4.writeObject(serviceListWithLocals);
//            oos5.writeObject(allWorkflowVersions);
//            oos6.writeObject(incompleteSimpleGraph);
//            oos7.writeObject(completeSimpleGraph);
//
//            inclompleteGraph = createServiceServiceGraph(incompleteSimpleGraph, serviceList, allWorkflowVersions);
//            completeGraph = createServiceServiceGraph(completeSimpleGraph, serviceListWithLocals, allWorkflowVersions);
//            oos.writeObject(inclompleteGraph);
//            oos2.writeObject(completeGraph);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            FileInputStream fout = new FileInputStream("incomplete-graph");
            FileInputStream fout2 = new FileInputStream("complete-graph");
            FileInputStream fout3 = new FileInputStream("service-list");
            FileInputStream fout4 = new FileInputStream("service-list-with-locals");
            FileInputStream fout5 = new FileInputStream("all-workflows");
            FileInputStream fout6 = new FileInputStream("incomplete-directed-simple-graph");
            FileInputStream fout7 = new FileInputStream("complete-directed-simple-graph");
            ObjectInputStream oos = new ObjectInputStream(fout);
            ObjectInputStream oos2 = new ObjectInputStream(fout2);
            ObjectInputStream oos3 = new ObjectInputStream(fout3);
            ObjectInputStream oos4 = new ObjectInputStream(fout4);
            ObjectInputStream oos5 = new ObjectInputStream(fout5);
            ObjectInputStream oos6 = new ObjectInputStream(fout6);
            ObjectInputStream oos7 = new ObjectInputStream(fout7);
            inclompleteGraph = (Graph<String, DefaultWeightedEdge>) oos.readObject();
            completeGraph = (Graph<String, DefaultWeightedEdge>) oos2.readObject();
            serviceList = (Set<SService>) oos3.readObject();
            serviceListWithLocals = (Set<SService>) oos4.readObject();
            allWorkflowVersions = (Set<WorkflowVersion>) oos5.readObject();
            incompleteSimpleGraph = (Graph<String, DefaultEdge>) oos6.readObject();
            completeSimpleGraph = (Graph<String, DefaultEdge>) oos7.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        for(WorkflowVersion workflowVersion: allWorkflowVersions){
            workflowVersion.setIntent();
        }


        initialServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
        newServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
    }

    private static void setServiceIntents(Set<SService>[] servicesTest1, Set<OOperation>[] operationsTest1) {
        for(int i=0; i<servicesTest1.length; i++){
            ArrayList<SService> serviceList = new ArrayList<SService>(servicesTest1[i]);
            for(SService serivce: serviceList){
                String intent = "";
                for(OOperation operation: operationsTest1[i]){
                    if (operation.getService().equals(serivce)) {
                        intent += operation.getProcessorName() + " " + operation.getName() + " ";
                    }
                }
                serivce.setIntent(intent);
            }
        }
    }

    private static Graph<String, DefaultWeightedEdge> createServiceServiceGraph(Graph<String, DefaultEdge> graph, Set<SService> services, Set<WorkflowVersion> workflowVersions) {
        Map<String, SService> serviceMap = createServiceMap(services);
        Graph<String, DefaultWeightedEdge> weightedGraph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        int edgeNumber = 0;
        for (DefaultEdge edge : graph.edgeSet()) {
            print("Edge number:" + edgeNumber++);
            SService source = serviceMap.get(graph.getEdgeSource(edge));
            SService target = serviceMap.get(graph.getEdgeTarget(edge));
            weightedGraph.addVertex(source.getURL());
            weightedGraph.addVertex(target.getURL());
            if (source.equals(target)) {
                print("We have looooooooops X_X");
            }
            if (edgeNumber == 25) {
                print("HEEEEELP!!!!!");
            }

            Set<Integer> intersectedWorkflows = new HashSet<Integer>();
            ArrayList<WorkflowVersion> workflowVersionsList = new ArrayList<WorkflowVersion>(workflowVersions);
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
        return weightedGraph;
    }
}
