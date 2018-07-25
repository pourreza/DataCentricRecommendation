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
import static utilities.Printer.printJSONListInFile;
import static utilities.PythonInterpreter2.getCosineSimilarity;

public class DataCentricRecommendation {

    public static final int MAX_PATH_LENGTH = 5;
    private final double INTENT_IMPORTANCE = 0.4;
    private final double CONTEXT_IMPORTANCE = 0.4;
    private final double PATH_LENGTH_IMPORTANCE = 0.2;
    private final Graph<String, DefaultWeightedEdge> weightedGraph;
    private Graph<String, DefaultEdge> graph;
    private Set<WorkflowVersion> workflows;
    private Set<SService> services;
    private Map<String, SService> serviceMap;
    private Map<String, String> servicesContexts;

    public DataCentricRecommendation(Graph<String, DefaultWeightedEdge> completeGraph, Set<WorkflowVersion> workflowVersions, Set<SService> services, Map<String, SService> serviceMap) {
        this.weightedGraph = completeGraph;
        this.workflows = workflowVersions;
        this.serviceMap = serviceMap;
        this.services = services;
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
//        AllDirectedPaths<String, DefaultWeightedEdge> allDirectedPaths = new AllDirectedPaths<String, DefaultWeightedEdge>(weightedGraph);
//        List<GraphPath<String, DefaultWeightedEdge>> allPaths = allDirectedPaths.getAllPaths(source.getURL(), target.getURL(), false, weightedGraph.vertexSet().size());
//        double maxScore = 0;
//        for(GraphPath<String, DefaultWeightedEdge> path: allPaths) {
        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(graph);
        GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(source.getURL()).getPath(target.getURL());
        double pathScore = 0;
        if (path.getLength() < MAX_PATH_LENGTH) {
            int pathLength = path.getLength();
            for (DefaultEdge pathEdge : path.getEdgeList()) {
                String edgeSource = path.getGraph().getEdgeSource(pathEdge);
                String edgeTarget = path.getGraph().getEdgeTarget(pathEdge);
                double edgeWeight = weightedGraph.getEdgeWeight(weightedGraph.getEdge(edgeSource, edgeTarget));
                double intentSimilarity = intentSimilarity(edgeSource, edgeTarget);
                double contextSimilarity = contextSimilarity(edgeSource, edgeTarget);
                pathScore += ((intentSimilarity * INTENT_IMPORTANCE + CONTEXT_IMPORTANCE * contextSimilarity) * edgeWeight);
            }
            pathScore /= pathLength;
        }
        return pathScore;
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
        return getCosineSimilarity(serviceMap.get(service1).getIntent(), serviceMap.get(service2).getIntent());
    }

    public ArrayList<Pair<String, String>> recommend(ArrayList<Pair<String, String>> newEdgesSource) {
        System.out.println("New Edges: "+ newEdgesSource.size());
        ArrayList<Pair<String, String>> candidates = findCandidates(newEdgesSource);
        ArrayList<CandidateScore> candidateScores = new ArrayList<CandidateScore>();
        int maxTries = 50;
        if(candidates.size()<maxTries) {
            maxTries = candidates.size();
        }

        System.out.print("candidateSize: "+ candidates.size() + " maxTries: " + maxTries);
//        for(int i=0; i<maxTries; i++){
//            Random r = new Random(123);
//            int index  = r.nextInt(candidates.size());
//            double score = getScore(serviceMap.get(candidates.get(index).getFirst()), serviceMap.get(candidates.get(index).getSecond()));
//            CandidateScore cnd = new CandidateScore(candidates.get(index), score);
//            candidateScores.add(cnd);
//        }
        for(Pair<String, String> candidate: candidates){
            System.out.print(".");
            double score = getScore(serviceMap.get(candidate.getFirst()), serviceMap.get(candidate.getSecond()));
            CandidateScore cnd = new CandidateScore(candidate, score);
            candidateScores.add(cnd);
        }
        for(Pair<String, String> newPair: newEdgesSource){
            for(CandidateScore candidatePair: candidateScores){
                if(candidatePair.candidate.getFirst().equals(newPair.getFirst()) && candidatePair.candidate.getSecond().equals(newPair.getSecond())){
                    print("WEEEEEEE have it in candidate scoressss with index "+ candidateScores.indexOf(candidatePair));
                }
            }
        }
        System.out.println(" Done");
//        Collections.sort(candidateScores);
//        ArrayList<Pair<String, String>> recommendedCandidates = new ArrayList<Pair<String, String>>();
//        for(CandidateScore candidateScore: candidateScores){
//            recommendedCandidates.add(candidateScore.candidate);
//        }
        return getTop10(candidateScores);
    }

    private ArrayList<Pair<String, String>> getTop10(ArrayList<CandidateScore> candidateScores) {
        ArrayList<Pair<String, String>> sortedCandidates = new ArrayList<Pair<String, String>>();
        int total = 10;
        if(candidateScores.size()<total)
            total = candidateScores.size();
        for(int i=0; i<total; i++){
            CandidateScore max = null;
            for(CandidateScore candidateScore: candidateScores){
                if(max==null || candidateScore.d>max.d){
                    max = candidateScore;
                    print("This is the new max: "+ max.d);
                }
            }
            sortedCandidates.add(new Pair<String, String>(max.candidate.getFirst(), max.candidate.getSecond()));
            candidateScores.remove(max);
        }
        return sortedCandidates;
    }

    private ArrayList<Pair<String, String>> findCandidates(ArrayList<Pair<String, String>> newEdgesSource) {
        ArrayList<Pair<String, String>> candidates = new ArrayList<Pair<String, String>>();
        for(Pair<String, String> servicePair: newEdgesSource){
            String service1 = servicePair.getFirst();
            for(SService service2: services){
                if(!service1.equals(service2.getURL())){
                    String source = service1;
                    String target = service2.getURL();
                    if(target.equals(servicePair.getSecond())){
                        System.out.println("FOUUUUUUUUUUUUUNDDDDD ITTTTTTT");
                    }
                    if(graph.containsVertex(source)&& graph.containsVertex(target)){
                        if(!graph.containsEdge(source, target)){// If this edge was not previously in the graph
                            DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(graph);
                            GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(source).getPath(target);
                            if (path != null && path.getLength()<MAX_PATH_LENGTH) {
                                if(target.equals(servicePair.getSecond())){
                                    System.out.println("Added the Candidateeeeeeeeeee!!!");
                                }
                                candidates.add(new Pair<String, String>(source, target));
                            }
                        }
                    }
                }
            }
        }
        return candidates;
    }

    class CandidateScore implements Comparable{
        public Pair<String, String> candidate;
        public double d;

        public CandidateScore(Pair<String,String> candidate, double d){
            this.candidate = candidate;
            this.d = d;
        }


        public int compareTo(Object o) {
            return (int)(this.d - ((CandidateScore) o).d);
        }
    }
}
