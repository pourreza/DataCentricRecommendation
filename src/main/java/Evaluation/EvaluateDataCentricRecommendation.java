package Evaluation;

import DataCentricRecommendation.DataCentricRecommendation;
import javafx.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import serviceWorkflowNetwork.AnalyzeDataOptimized;
import serviceWorkflowNetwork.OOperation;
import serviceWorkflowNetwork.SService;
import serviceWorkflowNetwork.WorkflowVersion;

import java.io.*;
import java.util.ArrayList;
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
        serviceList = extractor.getAllServices();
        completeGraph = extractor.getDirectedServiceGraph(true);
        serviceListWithLocals = extractor.getAllServices();
        allWorkflowVersions = extractor.getAllWorkflowVersions();


        try {
            FileOutputStream fout = new FileOutputStream("incomplete-graph");
            FileOutputStream fout2 = new FileOutputStream("complete-graph");
            FileOutputStream fout3 = new FileOutputStream("service-list");
            FileOutputStream fout4 = new FileOutputStream("service-list-with-locals");
            FileOutputStream fout5 = new FileOutputStream("all-workflows");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
            ObjectOutputStream oos3 = new ObjectOutputStream(fout3);
            ObjectOutputStream oos4 = new ObjectOutputStream(fout4);
            ObjectOutputStream oos5 = new ObjectOutputStream(fout5);
            oos.writeObject(inclompleteGraph);
            oos2.writeObject(completeGraph);
            oos3.writeObject(serviceList);
            oos4.writeObject(serviceListWithLocals);
            oos5.writeObject(allWorkflowVersions);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileInputStream fout = new FileInputStream("test1-uowNetwork");
            FileInputStream fout2 = new FileInputStream("test1-allServices");
            FileInputStream fout3 = new FileInputStream("test1-allOperations");
            FileInputStream fout4 = new FileInputStream("test1-allWorkflows");
            FileInputStream fout5 = new FileInputStream("allSortedWorkflowWrappers");
            FileInputStream fout6 = new FileInputStream("allServices");
            ObjectInputStream oos = new ObjectInputStream(fout);
            ObjectInputStream oos2 = new ObjectInputStream(fout2);
            ObjectInputStream oos3 = new ObjectInputStream(fout3);
            ObjectInputStream oos4 = new ObjectInputStream(fout4);
            ObjectInputStream oos5 = new ObjectInputStream(fout5);
            ObjectInputStream oos6 = new ObjectInputStream(fout6);
            inclompleteGraph = (Graph<String, DefaultEdge>) oos.readObject();
            completeGraph = (Graph<String, DefaultEdge>) oos2.readObject();
            serviceList = (Set<SService>) oos3.readObject();
            serviceListWithLocals = (Set<SService>) oos4.readObject();
            allWorkflowVersions = (Set<WorkflowVersion>) oos5.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        initialServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
        newServicesIncidentMatrix = new HashMap<Pair<String, String>, Double>();
    }
}
