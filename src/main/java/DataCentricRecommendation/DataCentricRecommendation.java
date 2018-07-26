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

import static utilities.PythonInterpreter2.getCosineSimilarity;

public class DataCentricRecommendation {

    private final int MAX_PATH_LENGTH = 5;
    private double INTENT_IMPORTANCE;
    private double CONTEXT_IMPORTANCE;
    private double PATH_LENGTH_IMPORTANCE;
    private final Graph<String, DefaultWeightedEdge> weightedGraph;
    private final Graph<String, DefaultEdge> simpleGraph;
    private Graph<String, DefaultEdge> graph;
    private Set<WorkflowVersion> workflows;
    private Set<SService> services;
    private Map<String, SService> serviceMap;
    private Map<String, String> servicesContexts;

    private int numberOfCandidates;
    private ArrayList<CandidateScore> candidateScores;

    public DataCentricRecommendation(Graph<String, DefaultWeightedEdge> completeGraph, Set<WorkflowVersion> workflowVersions, Set<SService> services, Map<String, SService> serviceMap, Graph<String, DefaultEdge> simpleGraph, double intentWeigth, double contextWeight, double pathLengthWeight) {
        this.weightedGraph = completeGraph;
        this.workflows = workflowVersions;
        this.serviceMap = serviceMap;
        this.services = services;
        this.simpleGraph = simpleGraph;
        this.servicesContexts = new HashMap<String, String>();
        createSimpleGraph(weightedGraph);
        INTENT_IMPORTANCE = intentWeigth;
        CONTEXT_IMPORTANCE = contextWeight;
        PATH_LENGTH_IMPORTANCE = pathLengthWeight;

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
            pathScore += (PATH_LENGTH_IMPORTANCE/pathLength);
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

    public int recommend(ArrayList<Pair<String, String>> newEdgesSource) {
        ArrayList<Pair<String, String>> candidates = new ArrayList<Pair<String, String>>(findCandidates(newEdgesSource));
        numberOfCandidates = candidates.size();

        candidateScores = new ArrayList<CandidateScore>();
        System.out.print("candidateSize: "+ candidates.size());
        for(Pair<String, String> candidate: candidates){
            System.out.print(".");
            double score = getScore(serviceMap.get(candidate.getFirst()), serviceMap.get(candidate.getSecond()));
            CandidateScore cnd = new CandidateScore(candidate, score);
            candidateScores.add(cnd);
        }
        System.out.println("Done");
        Collections.sort(candidateScores, Collections.<CandidateScore>reverseOrder());
//        ArrayList<Pair<String, String>> recommendedCandidates = new ArrayList<Pair<String, String>>();
//        for(CandidateScore candidateScore: candidateScores){
//            recommendedCandidates.add(candidateScore.candidate);
//        }
//        ArrayList<Pair<String, String>> top15 = getTop15(candidateScores);
//        for(Pair<String, String> newPair: newEdgesSource) {
//            for (CandidateScore pair : candidateScores) {
//                if (pair.getFirst().equals(newPair.getFirst()) && pair.getSecond().equals(newPair.getSecond()))
//                {
//                    System.out.println("I have it in to 150000000000000000000000000000000000000000000");
//                }
//
//            }
//        }
        return numberOfCandidates;
    }

    private ArrayList<Pair<String, String>> getTop15(ArrayList<CandidateScore> candidateScores) {
//        ArrayList<Pair<String, String>> sortedCandidates = new ArrayList<Pair<String, String>>();
//        int total = 20;
//        if(candidateScores.size()<total)
//            total = candidateScores.size();
//        for(int i=0; i<total; i++){
//            CandidateScore max = null;
//            for(CandidateScore candidateScore: candidateScores){
//                if(max==null || candidateScore.d>max.d){
//                    max = candidateScore;
//                }
//            }
//            print("top 10 ranked: " +i + " is " + max.candidate + " "+ max.d);
//            sortedCandidates.add(new Pair<String, String>(max.candidate.getFirst(), max.candidate.getSecond()));
//            candidateScores.remove(max);
//        }
//        return sortedCandidates;
        return null;
    }

    private Set<Pair<String, String>> findCandidates(ArrayList<Pair<String, String>> newEdgesSource) {
        Set<Pair<String, String>> candidates = new HashSet<Pair<String, String>>();
        for(Pair<String, String> servicePair: newEdgesSource){
            String service1 = servicePair.getFirst();
            for(SService service2: services){
                if(!service1.equals(service2.getURL())){
                    String source = service1;
                    String target = service2.getURL();
                    if(graph.containsVertex(source)&& graph.containsVertex(target)){
                        if(!simpleGraph.containsEdge(source, target)){// If this edge was not previously in the graph
                            DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<String, DefaultEdge>(graph);
                            GraphPath<String, DefaultEdge> path = dijkstraAlg.getPaths(source).getPath(target);
                            if (path != null && path.getLength()<MAX_PATH_LENGTH) {
                                candidates.add(new Pair<String, String>(source, target));
                            }
                        }
                    }
                }
            }
        }
        return candidates;
    }

    public String getRanks(ArrayList<Pair<String, String>> potentialOnes) {
        String rankings = "";
        for(Pair<String, String> pair: potentialOnes){
            for(int rank=0; rank<candidateScores.size(); rank++){
                String source = pair.getFirst();
                String target = pair.getSecond();
                String candidateSource = candidateScores.get(rank).getCandidate().getFirst();
                String candidateTarget = candidateScores.get(rank).getCandidate().getSecond();
                if(source.equals(candidateSource) && target.equals(candidateTarget)){
                    rankings+=((rank+1)+"-");
                    break;
                }
            }
        }
        return rankings;
    }

    public String getScores(ArrayList<Pair<String, String>> potentialOnes) {
        String scores = "";
        for(Pair<String, String> pair: potentialOnes){
            for(int rank=0; rank<candidateScores.size(); rank++){
                String source = pair.getFirst();
                String target = pair.getSecond();
                String candidateSource = candidateScores.get(rank).getCandidate().getFirst();
                String candidateTarget = candidateScores.get(rank).getCandidate().getSecond();
                if(source.equals(candidateSource) && target.equals(candidateTarget)){
                    scores+=(candidateScores.get(rank).getScore()+"-");
                    break;
                }
            }
        }
        return scores;
    }

    public Double getMinScore() {
        return candidateScores.get(candidateScores.size()-1).getScore();
    }

    public Double getMaxScore() {
        return candidateScores.get(0).getScore();
    }
}
