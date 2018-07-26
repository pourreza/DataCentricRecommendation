package DataCentricRecommendation;

import org.apache.commons.math3.util.Pair;

public class CandidateScore implements Comparable<CandidateScore>{
    private Pair<String, String> candidate;
    private double score;

    public CandidateScore(Pair<String,String> candidate, double d){
        this.candidate = candidate;
        this.score = d;
    }

    public double getScore() {
        return score;
    }

    public Pair<String, String> getCandidate() {
        return candidate;
    }

    public int compareTo(CandidateScore o) {
        if( this.getScore() < o.getScore()){
            return -1;
        }
        if( this.getScore() > o.getScore() )
            return 1;
        return 0;
    }
}