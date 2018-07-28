package utilities;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import serviceWorkflowNetwork.WorkflowVersion;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class Printer {

    public static void print(String s) {
        System.out.println(s);
    }

    public static void printTensor(int[][][] tensor) {
        for (int i = 0; i < tensor[0][0].length; i++) {
            for (int j = 0; j < tensor.length; j++) {
                for (int k = 0; k < tensor[0].length; k++) {
                    if (tensor[j][k][i] != 0)
                        System.out.print(tensor[j][k][i] + " ");
                }
                System.out.println();
            }
            System.out.println("@@@@@@@@@@@@@@@@");
        }
    }

    public static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (double d : matrix[i]) {
                System.out.print(d + " ");
            }
            System.out.println();
        }
    }

    public static void printVector(double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            System.out.print(vector[i] + " ");
        }
    }

    public static void saveToExcel(String fileName, WorkflowVersion[] workflowVersions,double[] goalServicesNum, double[] precisions, double[] recalls, double[] fscores, double[] steps, double[] links, double[] hits, double[] structure, double[] workflowStructures, double[] recommendedGraphServices) {
        print("Started writing in excel file");

        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results Without Structure Similarity Test1");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Index");
        headerRow.createCell(1).setCellValue("Workflow Index");
        headerRow.createCell(2).setCellValue("Workflow Version Index");
        headerRow.createCell(3).setCellValue("Year");
        headerRow.createCell(4).setCellValue("Month");
        headerRow.createCell(5).setCellValue("Day");
        headerRow.createCell(6).setCellValue("Date");
        headerRow.createCell(7).setCellValue("Number of goal services");
        headerRow.createCell(8).setCellValue("Number of conceptual components");
        headerRow.createCell(9).setCellValue("Precision");
        headerRow.createCell(10).setCellValue("Recall");
        headerRow.createCell(11).setCellValue("F-Score");
        headerRow.createCell(12).setCellValue("Steps");
        headerRow.createCell(13).setCellValue("Links Need to be Decided");
        headerRow.createCell(14).setCellValue("Hits");
        headerRow.createCell(15).setCellValue("Recommended Structure");
        headerRow.createCell(16).setCellValue("Workflow Structure");

        for (int index = 0; index < precisions.length; index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(index+1);
            results.createCell(1).setCellValue(workflowVersions[index].getWorkflow().getIndex());
            results.createCell(2).setCellValue(workflowVersions[index].getVersionIndex());
            results.createCell(3).setCellValue(workflowVersions[index].getDate().getYear());
            results.createCell(4).setCellValue(workflowVersions[index].getDate().getMonth());
            results.createCell(5).setCellValue(workflowVersions[index].getDate().getDay());
            results.createCell(6).setCellValue(workflowVersions[index].getDate().getDate());
            results.createCell(7).setCellValue(goalServicesNum[index]);
            results.createCell(8).setCellValue(workflowVersions[index].getExternalOperations().size());
            results.createCell(9).setCellValue(precisions[index]);
            results.createCell(10).setCellValue(recalls[index]);
            results.createCell(11).setCellValue(fscores[index]);
            results.createCell(12).setCellValue(steps[index]);
            results.createCell(13).setCellValue(links[index]);
            results.createCell(14).setCellValue(hits[index]);
            results.createCell(15).setCellValue(structure[index]);
            results.createCell(16).setCellValue(workflowStructures[index]);
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(fileName);
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void saveToExcel(double[] precisions, double[] recalls, double[] fscores, ArrayList<Set<String>>[] oldTestHyperEdges, ArrayList<Set<String>>[] predictedTestHyperEdges, String fileName) {
        print("Started writing in excel file");

        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Precision");
        headerRow.createCell(1).setCellValue("Recall");
        headerRow.createCell(2).setCellValue("F-Score");

        for (int index = 0; index < precisions.length; index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(precisions[index]);
            results.createCell(1).setCellValue(recalls[index]);
            results.createCell(2).setCellValue(fscores[index]);
        }

        writeToFile(fileName, workbook);

//        Workbook workbook2 = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
//        Sheet sheet2 = workbook2.createSheet("Results");
//
//
//        int maxValue = oldTestHyperEdges[0].size();
//        for(int i=1 ; i<oldTestHyperEdges.length-1; i++){
//            if(maxValue<oldTestHyperEdges[i].size())
//                maxValue = oldTestHyperEdges[i].size();
//        }
//
//        Row headerRow2 = sheet2.createRow(0);
//        headerRow2.createCell(0).setCellValue("Old Hyperedges 0");
//        headerRow2.createCell(1).setCellValue("Predicted Old Hyperedges 0");
//        headerRow2.createCell(2).setCellValue("Old Hyperedges 1");
//        headerRow2.createCell(3).setCellValue("Predicted Old Hyperedges 1");
//        headerRow2.createCell(4).setCellValue("Old Hyperedges 2");
//        headerRow2.createCell(5).setCellValue("Predicted Old Hyperedges 2");
//        headerRow2.createCell(6).setCellValue("Old Hyperedges 3");
//        headerRow2.createCell(7).setCellValue("Predicted Old Hyperedges 3");
//        headerRow2.createCell(8).setCellValue("Old Hyperedges 4");
//        headerRow2.createCell(9).setCellValue("Predicted Old Hyperedges 4");
//        headerRow2.createCell(10).setCellValue("Old Hyperedges 5");
//        headerRow2.createCell(11).setCellValue("Predicted Old Hyperedges 5");
//        headerRow2.createCell(12).setCellValue("Old Hyperedges 6");
//        headerRow2.createCell(13).setCellValue("Predicted Old Hyperedges 6");

//        for(int i=0; i<maxValue; i++){
//            Row res = sheet2.createRow(i+1);
//            for(int j=0; j<oldTestHyperEdges.length-1; j++){
//                if(oldTestHyperEdges[j].size()>i){
//                    res.createCell(j*2).setCellValue(oldTestHyperEdges[j].get(i).toString());
//                }
//                else{
//                    res.createCell(j*2).setCellValue(" ");
//                }
//                if(predictedTestHyperEdges[j].size()>i){
//                    res.createCell(j*2+1).setCellValue(predictedTestHyperEdges[j].get(i).toString());
//                }
//                else{
//                    res.createCell(j*2+1).setCellValue(" ");
//                }
//            }
//        }
//
//        try {
//            FileOutputStream outputStream = new FileOutputStream("TestHyperedgesWithFewerTestsWithoutDuplications.xlsx");
//            workbook2.write(outputStream);
//            workbook2.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void printInFile(Set<String> conferencesJournals, String fileName) {
        ArrayList<String> cj = new ArrayList<String>(conferencesJournals);
        Collections.sort(cj);
        try {
            PrintWriter writer = new PrintWriter(fileName);
            for (String conference : cj) {
                writer.println(conference);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void printListInFile(ArrayList<String> cj, String fileName) {
        try {
            PrintWriter writer = new PrintWriter(fileName);
            for (String conference : cj) {
                writer.println(conference);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void printJSONListInFile(ArrayList<JSONObject> cj, String fileName) {
        try {
            PrintWriter writer = new PrintWriter(fileName);
            for (JSONObject jsonObject : cj) {
                writer.println(jsonObject);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void saveToExcel(ArrayList<Date> uniqueSortedDates, int[] newEdges, int[] numberOfPotentials, double[] recalls, String fileName) {
        print("Started writing in excel file");

        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Year");
        headerRow.createCell(1).setCellValue("Month");
        headerRow.createCell(2).setCellValue("Day");
        headerRow.createCell(3).setCellValue("Time");
        headerRow.createCell(4).setCellValue("Number of potential edges");
        headerRow.createCell(5).setCellValue("Number of new edges");
        headerRow.createCell(6).setCellValue("Recall");

        for (int index = 1; index < uniqueSortedDates.size()-1; index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(uniqueSortedDates.get(index).getYear());
            results.createCell(1).setCellValue(uniqueSortedDates.get(index).getMonth());
            results.createCell(2).setCellValue(uniqueSortedDates.get(index).getDate());
            results.createCell(3).setCellValue(uniqueSortedDates.get(index).getTime());
            results.createCell(4).setCellValue(numberOfPotentials[index]);
            results.createCell(5).setCellValue(newEdges[index]);
            results.createCell(6).setCellValue(recalls[index]);
        }

        writeToFile(fileName, workbook);
    }

    public static void saveToExcel(ArrayList<Date> uniqueSortedDates, ArrayList<Long> time, String fileName) {
        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Year");
        headerRow.createCell(1).setCellValue("Month");
        headerRow.createCell(2).setCellValue("Day");
        headerRow.createCell(3).setCellValue("Time");
        headerRow.createCell(4).setCellValue("Running Time");

        for (int index = 1; index < uniqueSortedDates.size()-1; index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(uniqueSortedDates.get(index).getYear());
            results.createCell(1).setCellValue(uniqueSortedDates.get(index).getMonth());
            results.createCell(2).setCellValue(uniqueSortedDates.get(index).getDate());
            results.createCell(3).setCellValue(uniqueSortedDates.get(index).getTime());
            results.createCell(4).setCellValue(time.get(index));
        }

        writeToFile(fileName, workbook);
    }

    private static void writeToFile(String fileName, Workbook workbook) {
        try {
            FileOutputStream outputStream = new FileOutputStream(fileName);
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveToExcel(ArrayList<Date> dates, ArrayList<Integer> totalReported, ArrayList<Integer> correctlyPredicted, ArrayList<Double> precisions, String fileName) {
        print("Started writing in excel file");

        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Year");
        headerRow.createCell(1).setCellValue("Month");
        headerRow.createCell(2).setCellValue("Day");
        headerRow.createCell(3).setCellValue("Time");
        headerRow.createCell(4).setCellValue("Number of correctly predicted edges");
        headerRow.createCell(5).setCellValue("Number of predicted edges");
        headerRow.createCell(6).setCellValue("Precision");

        for (int index = 0; index < dates.size(); index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(dates.get(index).getYear());
            results.createCell(1).setCellValue(dates.get(index).getMonth());
            results.createCell(2).setCellValue(dates.get(index).getDate());
            results.createCell(3).setCellValue(dates.get(index).getTime());
            results.createCell(4).setCellValue(correctlyPredicted.get(index));
            results.createCell(5).setCellValue(totalReported.get(index));
            results.createCell(6).setCellValue(precisions.get(index));
        }

        writeToFile(fileName, workbook);
    }

    public static void writeToFile(String str, String fileName) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(str);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveToExcel(ArrayList<Date> dates, ArrayList<Integer> allNewOnes, ArrayList<Integer> potentialEdges, ArrayList<Integer> candidateListSize, ArrayList<String> ranksInCandidateList, ArrayList<String> candidatesScores, ArrayList<Double> minScores, ArrayList<Double> maxScores, ArrayList<Long> time, String fileName) {
        print("Started writing in excel file");

        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Year");
        headerRow.createCell(1).setCellValue("Month");
        headerRow.createCell(2).setCellValue("Day");
        headerRow.createCell(3).setCellValue("Time");
        headerRow.createCell(4).setCellValue("New Ones");
        headerRow.createCell(5).setCellValue("Potentials");
        headerRow.createCell(6).setCellValue("CandidateSize");
        headerRow.createCell(7).setCellValue("Ranks");
        headerRow.createCell(8).setCellValue("Scores");
        headerRow.createCell(9).setCellValue("Min Score");
        headerRow.createCell(10).setCellValue("Max Score");
        headerRow.createCell(11).setCellValue("Duration");

        for (int index = 0; index < dates.size(); index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(dates.get(index).getYear());
            results.createCell(1).setCellValue(dates.get(index).getMonth());
            results.createCell(2).setCellValue(dates.get(index).getDate());
            results.createCell(3).setCellValue(dates.get(index).getTime());
            results.createCell(4).setCellValue(allNewOnes.get(index));
            results.createCell(5).setCellValue(potentialEdges.get(index));
            results.createCell(6).setCellValue(candidateListSize.get(index));
            results.createCell(7).setCellValue(ranksInCandidateList.get(index));
            results.createCell(8).setCellValue(candidatesScores.get(index));
            results.createCell(9).setCellValue(minScores.get(index));
            results.createCell(10).setCellValue(maxScores.get(index));
            results.createCell(11).setCellValue(time.get(index));
        }

        writeToFile(fileName, workbook);
    }

    public static void writeToFile(ArrayList<Double> distances, String transEDistancesResults, ArrayList<Integer> ranks) {
        print("Started writing in excel file");

        Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
        Sheet sheet = workbook.createSheet("Results");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Distance");
        headerRow.createCell(1).setCellValue("Ranks");

        for (int index = 0; index < distances.size(); index++) {
            Row results = sheet.createRow(index + 1);
            results.createCell(0).setCellValue(distances.get(index));
            results.createCell(1).setCellValue(ranks.get(index));
        }

        writeToFile(transEDistancesResults, workbook);
    }
}