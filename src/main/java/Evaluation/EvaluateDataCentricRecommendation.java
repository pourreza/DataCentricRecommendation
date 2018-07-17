package Evaluation;

import DataCentricRecommendation.DataCentricRecommendation;
import javafx.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import serviceWorkflowNetwork.AnalyzeDataOptimized;
import serviceWorkflowNetwork.SService;
import serviceWorkflowNetwork.WorkflowVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static utilities.Printer.print;

public class EvaluateDataCentricRecommendation {

    public static Graph<String, DefaultEdge> inclompleteGraph; // I call this incomplete because this does not have the local workers
    public static Graph<String, DefaultEdge> completeGraph;  // I call this complete because in this graph we have added everything as first class citizens and we have more nodes and edges

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


//        for(SService service1: serviceList){
//            for(SService service2: serviceList){
//                if(!service1.equals(service2)){
//
//                }
//            }
//        }

        //QUESTION: should I also get the values for the initial graph ?

        DataCentricRecommendation recommendationAlgorithm = new DataCentricRecommendation();

        for (SService service1 : serviceListWithLocals) {
            for (SService service2 : serviceListWithLocals) {
                if (!service1.equals(service2)) {
                    double score = recommendationAlgorithm.getScore(service1, service2);
                    newServicesIncidentMatrix.put(new Pair<String, String>(service1.getURL(), service2.getURL()), score);
                }
            }
        }

        for (Pair<String, String> servicePair : newServicesIncidentMatrix.keySet()) {
            if (newServicesIncidentMatrix.get(servicePair) > 0)
                newConnectedNodes++;
        }

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

    private static void init() {
        AnalyzeDataOptimized extractor = new AnalyzeDataOptimized();
        inclompleteGraph = extractor.getDirectedServiceGraph(false);
        completeGraph = extractor.getDirectedServiceGraph(true);
        serviceList = extractor.getAllServices(false);
        serviceListWithLocals = extractor.getAllServices(true);
        allWorkflowVersions = extractor.getAllWorkflowVersions();

        initialServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
        newServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
    }
}
