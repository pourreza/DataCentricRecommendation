package DataCentricRecommendation;

import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import serviceWorkflowNetwork.OOperation;
import serviceWorkflowNetwork.SService;
import serviceWorkflowNetwork.WorkflowVersion;

import java.util.*;

import static utilities.Printer.print;
import static utilities.PythonInterpreter2.getCosineSimilarity;

public class DataCentricRecommendation {

    private final Graph<String, DefaultWeightedEdge> weightedGraph;
    private Graph<String, DefaultEdge> graph;
    private ArrayList<Pair<String, String>> allReverseEdges;
    private Set<WorkflowVersion> workflows;
    private Map<String, SService> serviceMap;
    private Map<String, String> servicesContexts;

    public DataCentricRecommendation(Graph<String, DefaultWeightedEdge> completeGraph, Set<WorkflowVersion> workflowVersions, Map<String, SService> serviceMap) {
        this.weightedGraph = completeGraph;
        this.workflows = workflowVersions;
        this.serviceMap = serviceMap;
        this.servicesContexts = new HashMap<String, String>();
        createSimpleGraph(weightedGraph);
    }

    private void createSimpleGraph(Graph<String, DefaultWeightedEdge> weightedGraph) {
        graph = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        for(String vertex: weightedGraph.vertexSet()){
            graph.addVertex(vertex);
        }
        for(DefaultWeightedEdge weightedEdge: weightedGraph.edgeSet()){
            graph.addEdge(weightedGraph.getEdgeSource(weightedEdge), weightedGraph.getEdgeTarget(weightedEdge));
        }
    }

    public double getScore(SService source, SService target) {
        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(graph);
        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(source.getURL()).getPath(target.getURL());
//        AllDirectedPaths<String, DefaultWeightedEdge> allDirectedPaths = new AllDirectedPaths<String, DefaultWeightedEdge>(weightedGraph);
//        List<GraphPath<String, DefaultWeightedEdge>> allPaths = allDirectedPaths.getAllPaths(source.getURL(), target.getURL(), false, weightedGraph.vertexSet().size());
//        double maxScore = 0;
//        for(GraphPath<String, DefaultWeightedEdge> path: allPaths){
        if(path!=null) {
            Graph<String, DefaultEdge> pathGraph = path.getGraph();
            double pathScore = 0;
            int pathLength = pathGraph.edgeSet().size();
            String twoAwaySource = "";
            String twoAwayTarget = "";
            int twoAway = 0;
            int countLoops = 0;
            for (DefaultEdge pathEdge : pathGraph.edgeSet()) {
                String edgeSource = pathGraph.getEdgeSource(pathEdge);
                String edgeTarget = pathGraph.getEdgeTarget(pathEdge);
                if (twoAway % 2 == 0) {
                    if (edgeSource.equals(twoAwaySource) && edgeTarget.equals(twoAwayTarget)) {
                        countLoops++;
                    }
                }
                print("edge: "+ twoAway);
                twoAway++;
                twoAwaySource = edgeSource;
                twoAwayTarget = edgeTarget;
                double edgeWeight = weightedGraph.getEdgeWeight(weightedGraph.getEdge(edgeSource, edgeTarget));
                double intentSimilarity = intentSimilarity(edgeSource, edgeTarget);
                double contextSimilarity = contextSimilarity(edgeSource, edgeTarget);
                pathScore += (intentSimilarity * contextSimilarity * edgeWeight);
            }
            pathScore /= pathLength;
//            if(pathScore>maxScore) {
//                maxScore = pathScore;
//            }
//        }
            return pathScore;
        }
        return 0;
    }

    private double contextSimilarity(String service1, String service2) {
        String serviceContext1 = "";
        String serviceContext2 = "";
        if(servicesContexts.containsKey(service1)){
            serviceContext1 = servicesContexts.get(service1);
        }else{
            for (WorkflowVersion workflow : workflows) {
                ArrayList<OOperation> operations = workflow.getExternalOperations();
                for (OOperation operation : operations) {
                    if (operation.getService().getURL().equals(service1)) {
                        serviceContext1 += workflow.getIntent()+" ";
                        break;
                    }
                }
            }
            servicesContexts.put(service1, serviceContext1);
        }
        if(servicesContexts.containsKey(service2)){
            serviceContext2 = servicesContexts.get(service2);
        }else{
            for (WorkflowVersion workflow : workflows) {
                ArrayList<OOperation> operations = workflow.getExternalOperations();
                for (OOperation operation : operations) {
                    if (operation.getService().getURL().equals(service2)) {
                        serviceContext2 += workflow.getIntent()+" ";
                        break;
                    }
                }
                servicesContexts.put(service2, serviceContext2);
            }
        }
        return getCosineSimilarity(serviceContext1, serviceContext2);
    }

    private double intentSimilarity(String service1, String service2) {
//        return getCosineSimilarity(serviceMap.get(service1).getIntent(), serviceMap.get(service2).getIntent());
        return 1;
    }

    public Graph<String, DefaultWeightedEdge> addAllReverseEdges() {
        allReverseEdges = new ArrayList<Pair<String, String>>();
        Set<DefaultWeightedEdge> edges = weightedGraph.edgeSet();
        for(DefaultWeightedEdge edge: edges){
            allReverseEdges.add(new Pair<String, String>(weightedGraph.getEdgeTarget(edge), weightedGraph.getEdgeSource(edge)));
        }
        for(Pair<String, String> reverseEdge: allReverseEdges){
            weightedGraph.addEdge(reverseEdge.getFirst(), reverseEdge.getSecond());
        }
        return weightedGraph;
    }
}
