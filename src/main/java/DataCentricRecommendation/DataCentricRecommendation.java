package DataCentricRecommendation;

import org._3pq.jgrapht.edge.DirectedWeightedEdge;
import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import serviceWorkflowNetwork.OOperation;
import serviceWorkflowNetwork.SService;
import serviceWorkflowNetwork.WorkflowVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static utilities.PythonInterpreter2.getCosineSimilarity;

public class DataCentricRecommendation {

    private final Graph<String, DefaultWeightedEdge> graph;
    private ArrayList<Pair<String, String>> allReverseEdges;
    private Set<WorkflowVersion> workflows;
    private Map<String, SService> serviceMap;

    public DataCentricRecommendation(Graph<String, DefaultWeightedEdge> completeGraph, Set<WorkflowVersion> workflowVersions, Map<String, SService> serviceMap) {
        this.graph = completeGraph;
        this.workflows = workflowVersions;
        this.serviceMap = serviceMap;
    }

    public double getScore(SService source, SService target) {
        AllDirectedPaths<String, DefaultWeightedEdge> allDirectedPaths = new AllDirectedPaths<String, DefaultWeightedEdge>(graph);
        List<GraphPath<String, DefaultWeightedEdge>> allPaths = allDirectedPaths.getAllPaths(source.getURL(), target.getURL(), false, graph.vertexSet().size());
        double maxScore = 0;
        for(GraphPath<String, DefaultWeightedEdge> path: allPaths){
            Graph<String, DefaultWeightedEdge> pathGraph = path.getGraph();
            double pathScore = 0;
            int pathLength = pathGraph.edgeSet().size();
            for(DefaultWeightedEdge pathEdge: pathGraph.edgeSet()){
                String edgeSource = pathGraph.getEdgeSource(pathEdge);
                String edgeTarget = pathGraph.getEdgeTarget(pathEdge);
                double edgeWeight = graph.getEdgeWeight(pathEdge);
                pathScore += (intentSimilarity(edgeSource, edgeTarget)*contextSimilarity(edgeSource, edgeTarget)* edgeWeight);
            }
            pathScore /= pathLength;
            if(pathScore>maxScore) {
                maxScore = pathScore;
            }
        }
        return maxScore;
    }

    private double contextSimilarity(String service1, String service2) {
        String serviceContext1 = "";
        String serviceContext2 = "";
        for (WorkflowVersion workflow : workflows) {
            ArrayList<OOperation> operations = workflow.getExternalOperations();
            for (OOperation operation : operations) {
                if (operation.getService().getURL().equals(service1)) {
                    serviceContext1 += workflow.getIntent()+" ";
                    break;
                }
                if (operation.getService().getURL().equals(service2)) {
                    serviceContext2 += workflow.getIntent()+" ";
                    break;
                }
            }
        }
        return getCosineSimilarity(serviceContext1, serviceContext2);
    }

    private double intentSimilarity(String service1, String service2) {
        return getCosineSimilarity(serviceMap.get(service1).getIntent(), serviceMap.get(service2).getIntent());
    }

    public void addAllReverseEdges() {
        allReverseEdges = new ArrayList<Pair<String, String>>();
        Set<DefaultWeightedEdge> edges = graph.edgeSet();
        for(DefaultWeightedEdge edge: edges){
            allReverseEdges.add(new Pair<String, String>(graph.getEdgeTarget(edge), graph.getEdgeSource(edge)));
        }
        for(Pair<String, String> reverseEdge: allReverseEdges){
            graph.addEdge(reverseEdge.getFirst(), reverseEdge.getSecond());
        }
    }
}
