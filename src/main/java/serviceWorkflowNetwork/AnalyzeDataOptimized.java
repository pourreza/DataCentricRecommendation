package serviceWorkflowNetwork;

import Evaluation.EvaluateDataCentricRecommendation;
import Evaluation.WorkflowWrapper;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static utilities.Printer.print;
import static serviceWorkflowNetwork.ServiceType.*;

public class AnalyzeDataOptimized {

    public static final int UNIQUE_DATES_SIZE = EvaluateDataCentricRecommendation.NUMBER_OF_UNIQUE_DATES;
    public Set<SService> services = new HashSet<SService>();
    public Set<OOperation> operations = new HashSet<OOperation>();
    public Set<WorkflowVersion> workflowVersions = new HashSet<WorkflowVersion>();
    public Set<Workflow> workflows = new HashSet<Workflow>();
    public Set<Person> people = new HashSet<Person>();

    public Graph<String, DefaultEdge>[] directedServiceServiceGraph = new Graph[UNIQUE_DATES_SIZE];
    public Graph<OOperation, DefaultEdge>[] directedOperationOperationGraph = new Graph[UNIQUE_DATES_SIZE];

    public Set<SService>[] networkServices = new Set[UNIQUE_DATES_SIZE];
    public Set<OOperation>[] networkOperations = new Set[UNIQUE_DATES_SIZE];
    public Set<WorkflowVersion>[] networkWorkflowVersions = new Set[UNIQUE_DATES_SIZE];

    public int notFoundUsers = 0;
    public int useless = 0;
    boolean test1 = false;
    boolean test2 = false;

    boolean withLocals = false;
    private ArrayList<Date> uniqueSortedDates;

    public void extractDataFromWorkflows(boolean withLocals) throws ParserConfigurationException {
        init(withLocals);

        //This part for examining all the files in this directory
        File[] workflowFiles = allWorkflowFiles();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        for (File file : workflowFiles) {
            print(file.getName());
            if (file.getName().equals("3457-1.t2flow")) {
                print("hi");
            }
            WorkflowVersion workflowVersion = extractWorkflowMetadata(file);

            try {
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                boolean isXMl = false;
                NodeList processorNode;

                DirectedGraph<String, DefaultEdge> directedProcessorGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

                if (file.getName().contains(".xml")) {
                    processorNode = (NodeList) xpath.compile("/scufl/processor").evaluate(doc, XPathConstants.NODESET);
                    isXMl = true;
                } else {
                    String xpathStr = "/workflow/dataflow/processors/processor/activities/activity";
                    processorNode = (NodeList) xpath.compile(xpathStr).evaluate(doc, XPathConstants.NODESET);
                }
                workflowVersion.setNumberOfService(processorNode.getLength());
                workflowVersion.setNumberOfExternalServices(retrieveExternalServices(isXMl, processorNode, workflowVersion, directedProcessorGraph));

                //created directed graph - add edges to the graph
                NodeList links;
                if (file.getName().contains(".xml")) {
                    links = (NodeList) xpath.compile("/scufl/link").evaluate(doc, XPathConstants.NODESET);
                } else {
                    links = (NodeList) xpath.compile("/workflow/dataflow/datalinks/datalink").evaluate(doc, XPathConstants.NODESET);
                }
                workflowVersion.setDirectedProcessorGraph(directedProcessorGraph);
                createEdges(directedProcessorGraph, links, isXMl);
                createEdgesForDirectedGraphs(directedProcessorGraph, workflowVersion);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }

        printSummary();
    }

    private WorkflowVersion extractWorkflowMetadata(File file) {
        String worflowIndexAndVersion = file.getName().substring(0, file.getName().indexOf("."));
        int workflowIndex = Integer.parseInt(worflowIndexAndVersion.substring(0, worflowIndexAndVersion.lastIndexOf("-")));
        String workflowURL = "http://www.myexperiment.org/workflows/" + workflowIndex + ".html";
        int versionIndex = Integer.parseInt(worflowIndexAndVersion.substring(worflowIndexAndVersion.lastIndexOf("-") + 1));

        String workflowVersionURL = "http://www.myexperiment.org/workflows/" + workflowIndex + "/versions/" + versionIndex + ".html";
        Date creationDate = findDate(workflowVersionURL, true);
        Person person = findPerson(workflowVersionURL);
        String description = findDescription(workflowVersionURL);
        people.add(person);
        Workflow workflow = new Workflow(workflowURL, workflowIndex, person);
        WorkflowVersion workflowVersion = new WorkflowVersion(workflow, versionIndex, creationDate, workflowVersionURL, description);
        Date updateDate = findDate(workflowVersionURL, false);
        workflowVersion.setUpdateDate(updateDate);
        workflowVersions.add(workflowVersion);
        networkWorkflowVersions[dateIndex(workflowVersion)].add(workflowVersion);
        if (workflows.contains(workflow)) {
            for (Workflow w : workflows) {
                if (w.equals(workflow)) {
                    w.addVersion(workflowVersion);
                }
            }
        } else {
            workflows.add(workflow);
            workflow.addVersion(workflowVersion);
        }
        return workflowVersion;
    }

    private File[] allWorkflowFiles() {
        File dir = new File("/Users/Maryam/MyExperimentDataset/");
        return dir.listFiles();
    }

    private void init(boolean withLocals) {
        this.withLocals = withLocals;

        services = new HashSet<SService>();
        operations = new HashSet<OOperation>();
        workflowVersions = new HashSet<WorkflowVersion>();
        workflows = new HashSet<Workflow>();
        people = new HashSet<Person>();

        directedServiceServiceGraph = new Graph[UNIQUE_DATES_SIZE];
        directedOperationOperationGraph = new Graph[UNIQUE_DATES_SIZE];

        for (int i = 0; i < UNIQUE_DATES_SIZE; i++) {
            directedServiceServiceGraph[i] = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
            directedOperationOperationGraph[i] = new DefaultDirectedGraph<OOperation, DefaultEdge>(DefaultEdge.class);
            networkServices[i] = new HashSet<SService>();
            networkOperations[i] = new HashSet<OOperation>();
            networkWorkflowVersions[i] = new HashSet<WorkflowVersion>();
        }
    }

    public Graph<String, DefaultEdge>[] getDirectedServiceGraph(boolean withLocals, ArrayList<Date> uniqueSortedDates) {
        this.uniqueSortedDates = new ArrayList<Date>(uniqueSortedDates);
        try {
            extractDataFromWorkflows(withLocals);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return directedServiceServiceGraph;
    }

    public Set<SService>[] getAllServices() {
        print("Combine Services");
        for(int i=0; i<UNIQUE_DATES_SIZE; i++){
            for(int j=i+1; j<UNIQUE_DATES_SIZE; j++){
                networkServices[j].addAll(networkServices[i]);
            }
        }
        return networkServices;
    }

    public Set<WorkflowVersion>[] getAllWorkflowVersions() {
        print("Combine Workflows");
        for(int i=0; i<UNIQUE_DATES_SIZE; i++){
            for(int j=i+1; j<UNIQUE_DATES_SIZE; j++){
                networkWorkflowVersions[j].addAll(networkWorkflowVersions[i]);
            }
        }
        return networkWorkflowVersions;
    }

    private void createEdgesForDirectedGraphs(Graph<String, DefaultEdge> directedProcessorGraph, WorkflowVersion workflowVersion) {
        for (int i = 0; i < workflowVersion.getExternalOperations().size(); i++) {
            OOperation source = workflowVersion.getExternalOperations().get(i);
            for (int j = 0; j < workflowVersion.getExternalOperations().size(); j++) {
                if (!workflowVersion.getExternalOperations().get(j).equals(workflowVersion.getExternalOperations().get(i))) {
                    OOperation sink = workflowVersion.getExternalOperations().get(j);
                    GraphPath<String, DefaultEdge> path = DijkstraShortestPath.findPathBetween(directedProcessorGraph, source.getProcessorName(), sink.getProcessorName());
                    if (path != null && !source.getService().equals(sink.getService())) {
                        directedServiceServiceGraph[dateIndex(workflowVersion)].addEdge(source.getService().getURL(), sink.getService().getURL());
                        directedOperationOperationGraph[dateIndex(workflowVersion)].addEdge(source, sink);
                    }
                }
            }
        }
    }

    private void createEdges(Graph<String, DefaultEdge> directedProcessorGraph, NodeList links, boolean isXML) {
        ///////for every link
        for (int i = 0; i < links.getLength(); i++) {
            String sink = null;
            String source = null;
            if (isXML) {
                String sinkStr = links.item(i).getAttributes().getNamedItem("sink").getTextContent().trim();
                if (sinkStr.contains(":"))
                    sink = sinkStr.substring(0, sinkStr.lastIndexOf(":"));
                else
                    sink = sinkStr;
                String sourceStr = links.item(i).getAttributes().getNamedItem("source").getTextContent().trim();
                if (sourceStr.contains(":"))
                    source = sourceStr.substring(0, sourceStr.lastIndexOf(":"));
                else
                    source = sourceStr;

            } else {
                NodeList childNodes = links.item(i).getChildNodes();
                //////// for every element in the activity
                for (int j = 0; j < childNodes.getLength(); j++) {
                    NodeList grandChildren = childNodes.item(j).getChildNodes();
                    for (int k = 0; k < grandChildren.getLength(); k++) {
                        if (grandChildren.item(k).getNodeName().equals("processor")) {
                            if (childNodes.item(j).getNodeName().equals("sink")) {
                                sink = grandChildren.item(k).getTextContent().trim();
                            } else if (childNodes.item(j).getNodeName().equals("source")) {
                                source = grandChildren.item(k).getTextContent().trim();
                            }
                        }
                    }
                }
            }
            if (directedProcessorGraph.containsVertex(source) && directedProcessorGraph.containsVertex(sink))
                directedProcessorGraph.addEdge(source, sink);
        }
    }

    private int retrieveExternalServices(boolean isXMl, NodeList processorNode, WorkflowVersion workflowVersion, Graph<String, DefaultEdge> directedProcessorGraph) {
        int numExternalServices = 0;
        ///////for every processor
        for (int i = 0; i < processorNode.getLength(); i++) {
            String serviceTypeStr = null;
            String processorName = null;
            NodeList childNodes = processorNode.item(i).getChildNodes();
            if (isXMl) {
                processorName = processorNode.item(i).getAttributes().getNamedItem("name").getTextContent();
                directedProcessorGraph.addVertex(processorName);
            } else {
                processorName = findProcessorName(processorNode.item(i));
                directedProcessorGraph.addVertex(processorName);
            }
            workflowVersion.addProcessorNames(processorName);
            //////// for every element in the activity
            for (int j = 0; j < childNodes.getLength(); j++) {
                if (isXMl) {
                    serviceTypeStr = childNodes.item(j).getNodeName();
                    if (childNodes.item(j).getNodeName().equals("s:local")) {
                        serviceTypeStr = "s:local";
                    }
                } else {
                    ///// if the inside element is raven then it includes the artifact which means the type of service
                    if (childNodes.item(j).getNodeName().equals("raven")) {
                        NodeList childNodes1 = childNodes.item(j).getChildNodes();
                        for (int k = 0; k < childNodes1.getLength(); k++) {
                            if (childNodes1.item(k).getNodeName().equals("artifact")) {
                                serviceTypeStr = childNodes1.item(k).getTextContent().trim().toLowerCase();
                            }
                        }
                    }
                }
                if (childNodes.item(j) != null) {
                    NodeList insideNodes = childNodes.item(j).getChildNodes();
                    numExternalServices = digForURL(workflowVersion, numExternalServices, serviceTypeStr, insideNodes, processorName);
//                    print(childNodes.item(j).getNodeName());
                }
                if (childNodes.item(j).getChildNodes().item(0) != null) {
                    NodeList insideNodes = childNodes.item(j).getChildNodes().item(0).getChildNodes();
                    numExternalServices = digForURL(workflowVersion, numExternalServices, serviceTypeStr, insideNodes, processorName);
//                    print(childNodes.item(j).getChildNodes().item(0).getNodeName());
                }
            }

        }
        return numExternalServices;
    }

    private static String findProcessorName(Node grandchildNode) {
        Node processorNode = grandchildNode.getParentNode().getParentNode();
        NodeList childrenNodes = processorNode.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); i++) {
            if (childrenNodes.item(i).getNodeName().equals("name")) {
                return childrenNodes.item(i).getTextContent();
            }
        }
        return null;
    }

    private int digForURL(WorkflowVersion workflowVersion, int numExternalServices, String serviceTypeStr, NodeList insideNodes, String processorName) {
        for (int k = 0; k < insideNodes.getLength(); k++) {
            Node URLnode = insideNodes.item(k);
            if (URLnode.getTextContent().startsWith("http://") || URLnode.getTextContent().startsWith("https://")) {
                ServiceType serviceType = findServiceType(serviceTypeStr);
                if (serviceType != null)
                    if (createOperationService(serviceType, processorName, k, URLnode, workflowVersion)) {
                        numExternalServices++;
                        break;
                    }
            } else if (withLocals) {
                if (URLnode.getParentNode().getNodeName().equals("s:local") || (serviceTypeStr != null && serviceTypeStr.contains("localworker-") && URLnode.getNodeName().equals("localworkerName"))) {
                    ServiceType serviceType = LOCAL;
                    if (createOperationService(serviceType, processorName, k, URLnode, workflowVersion)) {
                        numExternalServices++;
                        break;
                    }
                }
            }
        }
        return numExternalServices;
    }

    private boolean createOperationService(ServiceType serviceType, String processorName, int k, Node URLnode, WorkflowVersion workflowVersion) {
        String serviceURL = URLnode.getTextContent().trim();
        String operationName = null;

        if (serviceType.equals(WSDL) || serviceType.equals(BIOMOBY) || serviceType.equals(ARBITRARYGT4)) {
            operationName = URLnode.getParentNode().getChildNodes().item(k + 2).getTextContent();
        } else if (serviceType.equals(SOAPLAB)) {
            operationName = serviceURL.substring(serviceURL.lastIndexOf(".") + 1);
            serviceURL = serviceURL.substring(0, serviceURL.lastIndexOf("."));
        } else if (serviceType.equals(LOCAL) && withLocals) {
            operationName = serviceURL;
        } else if (serviceType.equals(REST)) {
            String initialURL = serviceURL;
            boolean hasSpecialMark = false;
            if (serviceURL.lastIndexOf("?") != -1) {
                serviceURL = serviceURL.substring(0, serviceURL.lastIndexOf("?"));
                hasSpecialMark = true;
            }
            if (serviceURL.indexOf("{") != -1) {
                serviceURL = serviceURL.substring(0, serviceURL.indexOf("{"));
                hasSpecialMark = true;
            }
            if (serviceURL.charAt(serviceURL.length() - 1) == '/')
                serviceURL = serviceURL.substring(0, serviceURL.length() - 1);

            if (hasSpecialMark && serviceURL.length() > 7 && serviceURL.substring(7).lastIndexOf("/") == serviceURL.substring(7).indexOf("/")) {// this means that we have ? or { and before these marks and after http:// there is NO more than one change of directory with more than one slash
                operationName = serviceURL;
            } else {
                operationName = serviceURL.substring(serviceURL.lastIndexOf("/") + 1);
                serviceURL = serviceURL.substring(0, serviceURL.lastIndexOf("/"));
                if (operationName.trim().equals("")) {
                    print("Initial URL: " + initialURL);
                    print("Operation Name: " + operationName);
                    print("Service URL: " + serviceURL);
                }
            }
        } else if (serviceType.equals(SADI)) {
            operationName = serviceURL.substring(serviceURL.lastIndexOf("/") + 1);
        }

        if (createNetworkNodesRelations(serviceType, processorName, workflowVersion, serviceURL, operationName))
            return true;
        return false;
    }

    private boolean createNetworkNodesRelations(ServiceType serviceType, String processorName, WorkflowVersion workflowVersion, String serviceURL, String operationName) {
        if (operationName != null && !operationName.equals("") && serviceURL != null) {
            SService service = new SService(serviceURL, serviceType);
            services.add(service);
            networkServices[dateIndex(workflowVersion)].add(service);
            OOperation operation = new OOperation(service, operationName, processorName);
            operations.add(operation);
            networkOperations[dateIndex(workflowVersion)].add(operation);

            directedServiceServiceGraph[dateIndex(workflowVersion)].addVertex(serviceURL);
            directedOperationOperationGraph[dateIndex(workflowVersion)].addVertex(operation);

            workflowVersion.addExternalOperation(operation);
            return true;
        }
        return false;
    }

    private int dateIndex(WorkflowVersion workflowVersion) {
        return uniqueSortedDates.indexOf(workflowVersion.getDate());
    }

    private void printSummary() {
        ArrayList<WorkflowVersion> workflowVersionList = new ArrayList<WorkflowVersion>(workflowVersions);
        print("number of workflows: " + workflows.size());
        print("number of workflowVersions: " + workflowVersionList.size());
        int countMore = 0;
        int countOnes = 0;
        int countTwos = 0;
        int countThrees = 0;
        int countFours = 0;
        int countFives = 0;
        int countTens = 0;
        int countMoreThan10 = 0;
        int countMoreThan100 = 0;
        int countVersions = 0;
        int countMore5 = 0;
        for (int i = 0; i < workflowVersionList.size(); i++) {
            WorkflowVersion workflowVersion = workflowVersionList.get(i);
//            if(workflowVersion.getWorkflow().getVersions().size()==workflowVersion.getVersionIndex()) {
            countVersions++;
            if (workflowVersion.getNumExternalServices() >= 1) {
                countMore++;
            }
            if (workflowVersion.getNumExternalServices() == 1)
                countOnes++;
            if (workflowVersion.getNumExternalServices() == 2)
                countTwos++;
            if (workflowVersion.getNumExternalServices() == 3)
                countThrees++;
            if (workflowVersion.getNumExternalServices() == 4)
                countFours++;
            if (workflowVersion.getNumExternalServices() == 5)
                countFives++;

            if (workflowVersion.getNumExternalServices() > 5 && workflowVersion.getNumExternalServices() < 10)
                countMore5++;
            if (workflowVersion.getNumExternalServices() == 10)
                countTens++;
            if (workflowVersion.getNumExternalServices() > 10) {
                print("More than 10 : " + workflowVersion.getNumExternalServices() + " ");
                countMoreThan10++;
            }
            if (workflowVersion.getNumExternalServices() > 100) {
                print("More than 100 : " + workflowVersion.getNumExternalServices());//+ " " + workflowVersion.getWorkflowIndex() + " versionIndex: " + workflowVersion.getVersionIndex());
                countMoreThan100++;
            }
//            }
        }

        print("All versions considered: " + countVersions);
        print("numb external services more than one: " + countMore);

        print("ones: " + countOnes);
        print("Twos: " + countTwos);
        print("Threes: " + countThrees);
        print("Fours: " + countFours);
        print("Fives: " + countFives);
        print("Tens: " + countTens);
        print("More than 5: " + countMore5);
        print("More than 10: " + countMoreThan10);
        print("More than 100: " + countMoreThan100);

        print("Number of services: " + services.size());
        print("Number of operations: " + operations.size());

        double sumVersions = 0;
        double sumServices = 0;
        double sumOperations = 0;
        double hasMoreOperations = 0;
        for (Workflow workflow : workflows) {
            sumVersions += workflow.getVersions().size();
            if (workflow.getVersions().size() > 1)
                hasMoreOperations++;
            //external operations for last version
            ArrayList<OOperation> externalOperations = workflow.getVersions().get(workflow.getVersions().size() - 1).getExternalOperations();
            sumOperations += externalOperations.size();
            Set<SService> serviceSet = new HashSet<SService>();
            for (OOperation operation : externalOperations) {
                serviceSet.add(operation.getService());
            }
            sumServices += serviceSet.size();
        }

        print(hasMoreOperations + "has more operations");

        print(people.size() + " peopless ");
        print(sumVersions / workflows.size() + " avg version per workflow ");
        print(sumOperations / workflows.size() + " avg operation per workflow ");
        print(sumServices / workflows.size() + " avg services per workflow ");

        print(minMaxDate(workflows));
        double sumWorkflows = 0;
        for (Person person : people) {
            int workflowsForperson = 0;
            for (Workflow workflow : workflows) {
                if (workflow.getContributors().contains(person)) {
                    workflowsForperson++;
                }
            }
            sumWorkflows += workflowsForperson;
        }
        print(sumWorkflows / people.size() + " avg workflows per person ");

        print("not useless: " + useless);
        print("not Found Users: " + notFoundUsers);
    }

    private String minMaxDate(Set<Workflow> workflows) {
        int minyear = 2018;
        int maxYear = 1800;
        for (WorkflowVersion workflow : workflowVersions) {
            if (workflow.getDate().getYear() < minyear)
                minyear = workflow.getDate().getYear();
            if (workflow.getDate().getYear() > maxYear)
                maxYear = workflow.getDate().getYear();
        }
        return "Min Year: " + minyear + " Max Year: " + maxYear;
    }

    private Person findPerson(String workflowURL) {
        String csvFile = "feedmeWithVersionsAll";
        BufferedReader br = null;
        String line = "";
        try {

            br = new BufferedReader(new FileReader(csvFile));
            line = br.readLine();
            int lineNumber = 2;
            while ((line = br.readLine()) != null) {
                String[] data2 = line.split("\"");
                if (workflowURL.equals(data2[3])) {
                    int id = Integer.parseInt(data2[33].substring(data2[33].lastIndexOf("/") + 1));
                    String name = data2[39];
                    return new Person(name, id);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        print("Person NOT FOUNDDDDDDDDDDDDDDDDDDDDDD");
        return null;
    }

    private String findDescription(String workflowURL) {
        String csvFile = "feedmeWithVersionsAll";
        BufferedReader br = null;
        String line = "";
        try {

            br = new BufferedReader(new FileReader(csvFile));
            line = br.readLine();
            int lineNumber = 2;
            while ((line = br.readLine()) != null) {
                String[] data2 = line.split("\"");
                if (workflowURL.equals(data2[3])) {
                    if (data2.length > 9) {
                        String description = data2[63];
                        return description;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Date findDate(String workflowURL, boolean isCreation) {
        String csvFile = "feedmeWithVersionsAll";
        BufferedReader br = null;
        String line = "";
        try {

            br = new BufferedReader(new FileReader(csvFile));
            line = br.readLine();
            int lineNumber = 2;
            while ((line = br.readLine()) != null) {
                String[] data2 = line.split("\"");
                //Creation date
                if (data2.length < 60) {
                    print(line);
                }
                if (workflowURL.equals(data2[3])) {
                    //update date
                    String date = data2[15];

                    if (!isCreation) {
                        date = data2[21];
                        if (date == null || date.trim().equals(""))
                            date = data2[15].trim();
                    }

                    int year = Integer.parseInt(date.substring(0, 4));
                    int month = Integer.parseInt(date.substring(5, 7));
                    int days = Integer.parseInt(date.substring(8, 10));
                    return new Date(year, month, days);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        print("NOT FOUUUUUUUUUUUUUUUUUUUUUUUUD DATE");
        return null;
    }

    public Set<OOperation>[] getAllOperations() {
        print("Combine Operations");
        for(int i=0; i<UNIQUE_DATES_SIZE; i++){
            for(int j=i+1; j<UNIQUE_DATES_SIZE; j++){
                networkOperations[j].addAll(networkOperations[i]);
            }
        }
        return networkOperations;
    }
}