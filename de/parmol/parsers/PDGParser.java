package de.parmol.parsers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.parmol.Settings;
import de.parmol.util.*;
import de.parmol.FFSM.*;
import de.parmol.graph.*;


/**
 * �������PDGͼ�Ľ�������PDGͼת��ΪEPDGͼ����ӹ������������ߣ�SDDE����Ϊ���ܴ��ڵĶ�����������ת��Ϊ����ͼ��
 * �������EPDGͼת��Ϊ����ͼ��ͬʱΪ���еı���ӱ�ǩ
 * 
 * @author ���� ����
 * 
 * 
 */
public class PDGParser implements GraphParser {

	/*����ͼ����PDGͼ�ĸ���*/
	public static int inputGraphCount;
	
	/* ����PDGͼ���������� */
	public final static PDGParser instance = new PDGParser();

	/* ��������ʱ��PDGͼת��ΪEPDG֮���EPDGͼ�� */
	public final static LinkedList<DirectedListGraph> epdgGraphs = new LinkedList<DirectedListGraph>();

	/* ���淽���ж�Ӧ�ı�ǩ��Ӧ��ϵ���ԣ������������������������������� ����ͼ�ͽڵ㣬�ڵ��ǩ�����*/
	private static HashMap<String, HashMap<Integer, String>> labelMap = new HashMap<String, HashMap<Integer, String>>();
	
	public static HashMap<String, String> LabelStatementMap = new HashMap<String, String>();
	
	private static HashMap<String, Set<String>> LabelStatementSet = new HashMap<String, Set<String>>();
	//private static HashMap<String, String> LabelNameMap = new HashMap<String, String>();
	

	/* ���ֱߵ�������ϵ��Ӧ�ı�ǩ */

	// �ھ������߱�ǩ
	private final static char CTRL_EDGE = 'c';
	// ���������߱�ǩ
	private final static char DATA_EDGE = 'd';
	// �������������߱�ǩ
	private final static char SHARED_EDGE = 's';

	/* ��ӵ�������ı�ǩ */
	private static int VIRTUAL_NODE = Integer.MAX_VALUE;

	/* ����PDG��Ϣ��XML�ļ��еĽ�� */
	private final static String METHOD_NAME = "Name";

	private final static String CLASS_NAME = "Name";

	private final static String NODES = "nodes";

	private final static String NODE = "Statement";

	private final static String NODE_NAME = "no";

	private final static String STATEMENT = "statement";

	private final static String NODE_LABEL = "nodelabel";

	private final static String CTRL_DEPENDENCE = "control_dependece";

	private final static String DATA_DEPENDENCE = "data_dependece";

	private final static String DEPENDEE = "dependee";

	private final static String DEPENDER = "depender";

	private final static String STMT_NO = "no";

	public boolean directed() {
		return false;
	}

	/*
	 * �ڸý������е���creatGraph()������������ͼ����graph���е�����ͼֻ�Ǽ̳���Graph�ӿڶ��ѣ�������ǿ��ת��֮��
	 * �൱�ڹ������������͵�ͼ
	 * (non-Javadoc)
	 * @see de.parmol.parsers.GraphParser#getDesiredGraphFactoryProperties()
	 */
	public int getDesiredGraphFactoryProperties() {
		return GraphFactory.UNDIRECTED_GRAPH;
	}

	public String getNodeLabel(int nodeLabel) {
		throw new UnsupportedOperationException("��֧�ֵĲ�����");
	}

	/**
	 * ����һ������һ�������������ͼ
	 * @param method ����ĳ��������Ӧ��Element
	 * @param factory ����ĳ�����͵�ͼ����settingָ��
	 * @param className ���������������
	 * @return ������Ӧ��EPDG������������ͼ
	 * @throws JDOMException
	 */
	public DirectedListGraph parse(Element method, String className, GraphFactory factory) throws JDOMException {
		String methodName = className + "." + method.getAttributeValue(METHOD_NAME);
		DirectedListGraph g = (DirectedListGraph) factory.createGraph(methodName);

		//����->��ǩ
		HashMap<String, Integer> nodeMap = new HashMap<String, Integer>();
		//����->���
		HashMap<Integer, String> statementMap = new HashMap<Integer, String>();
		Set<String> statementSet = new HashSet<String>();

		Element method_node = method.getChild(NODES);
		List<?> nodesLst = method_node.getChildren(NODE);
		int index = 0;
		for (Iterator<?> nodeIter = nodesLst.iterator(); nodeIter.hasNext();) {
			Element node = (Element) nodeIter.next();
			String nodeName = node.getAttributeValue(NODE_NAME);
			String nodeLabel = node.getAttributeValue(NODE_LABEL);
			int int_nodeLabel = Integer.parseInt(nodeLabel);

			// ����½��
			index = g.addNode(int_nodeLabel);
			// �����������붥��������ӳ���ϵ
			nodeMap.put(nodeName, new Integer(index));
			// �������������������֮���ӳ���ϵ
			statementMap.put(new Integer(index), node.getAttributeValue(STATEMENT));
			
			if (LabelStatementSet.containsKey(nodeLabel)) {
				LabelStatementSet.get(nodeLabel).add(node.getAttributeValue(STATEMENT));
			} else {
				LabelStatementSet.put(nodeLabel, new HashSet());
				LabelStatementSet.get(nodeLabel).add(node.getAttributeValue(STATEMENT));
			}
			//���ȫ�ֵĶ����ǩ�����Ķ�Ӧ��ϵ��
			LabelStatementMap.put(nodeLabel, node.getAttributeValue(STATEMENT));
			//LabelNameMap.put(nodeLabel, nodeName);
		}
		labelMap.put(methodName, statementMap);

		Element control_node = method.getChild(CTRL_DEPENDENCE);
		// ��������������
		if (control_node != null) {
			List<?> controlLst = control_node.getChildren(DEPENDEE);
			for (Iterator<?> ctrlIter = controlLst.iterator(); ctrlIter.hasNext();) {
				Element dependee = (Element) ctrlIter.next();
				String nodeA_name = dependee.getAttributeValue(STMT_NO);
				int nodeA = ((Integer) nodeMap.get(nodeA_name)).intValue();
				List<?> dependerLst = dependee.getChildren(DEPENDER);
				for (Iterator<?> dIter = dependerLst.iterator(); dIter.hasNext();) {
					Element depender = (Element) dIter.next();
					String nodeB_name = depender.getAttributeValue(STMT_NO);
					int nodeB = ((Integer) nodeMap.get(nodeB_name)).intValue();
					
					//��ߣ���д��ĸ  С�ߣ�Сд��ĸ
					if( (g.getNodeLabel(nodeA) > g.getNodeLabel(nodeB)) ){
						g.addEdge(nodeA, nodeB, 'C');
					} else{
						g.addEdge(nodeA, nodeB, CTRL_EDGE);
					}
					
					/*g.addEdge(nodeA, nodeB, CTRL_EDGE);*/

				}
			}
		}

		LinkedList<LinkedList<Integer>> dataDepSet = new LinkedList<LinkedList<Integer>>(); // ��¼��������ָ����ļ��ϵļ���
		Element data_node = method.getChild(DATA_DEPENDENCE);
		// ��������������
		if (data_node != null) {
			List<?> dataLst = data_node.getChildren(DEPENDEE);
			for (Iterator<?> dataIter = dataLst.iterator(); dataIter.hasNext();) {
				Element dependee = (Element) dataIter.next();
				String nodeA_name = dependee.getAttributeValue(STMT_NO);
				int nodeA = ((Integer) nodeMap.get(nodeA_name)).intValue();

				//��¼�����ڽڵ�nodeA�Ľڵ��ǩ
				LinkedList<Integer> dataDepNodes = new LinkedList<Integer>();
				List<?> dependerLst = dependee.getChildren(DEPENDER);
				for (Iterator<?> dIter = dependerLst.iterator(); dIter.hasNext();) {
					Element depender = (Element) dIter.next();
					String nodeB_name = depender.getAttributeValue(STMT_NO);
					int nodeB = ((Integer) nodeMap.get(nodeB_name)).intValue();

					// �õ���������ָ��Ľ�㼯��
					dataDepNodes.add(new Integer(nodeB));

					// �����ת��Ϊ����
					if (g.getEdge(nodeA, nodeB) == Graph.NO_EDGE && g.getEdge(nodeB, nodeA) == Graph.NO_EDGE) {
						//��ߣ���д��ĸ  С�ߣ�Сд��ĸ
						if( (g.getNodeLabel(nodeA) > g.getNodeLabel(nodeB)) ){
							g.addEdge(nodeA, nodeB, 'D');
						} else{
							g.addEdge(nodeA, nodeB, DATA_EDGE);
						}
						
						/*g.addEdge(nodeA, nodeB, DATA_EDGE);*/
						
					} else if (g.getEdge(nodeA, nodeB) != Graph.NO_EDGE || g.getEdge(nodeB, nodeA) != Graph.NO_EDGE) {
						int nodeC = g.addNode(VIRTUAL_NODE--);
						
						//��ߣ���д��ĸ  С�ߣ�Сд��ĸ
						if( (g.getNodeLabel(nodeA) > g.getNodeLabel(nodeB)) ){
							g.addEdge(nodeA, nodeC, 'D');
							g.addEdge(nodeC, nodeB, 'D');
						} else{
							g.addEdge(nodeA, nodeC, DATA_EDGE);
							g.addEdge(nodeC, nodeB, DATA_EDGE);
						}
						
						/*g.addEdge(nodeA, nodeC, DATA_EDGE);
						g.addEdge(nodeC, nodeB, DATA_EDGE);*/
						
					}
				}
				dataDepSet.add(dataDepNodes);
			}
		}

		// ��ӹ�������������(SDDE)
		for (Iterator<LinkedList<Integer>> dsIter = dataDepSet.iterator(); dsIter.hasNext();) {
			LinkedList<?> dsLst = (LinkedList<?>) dsIter.next();
			if (dsLst.size() > 1) {
				Object[] ds = dsLst.toArray();
				for (int i = 0; i < ds.length - 1; i++) {
					int nodeA = ((Integer) ds[i]).intValue();
					// ��ȡnodeA�����������Ƚ��
					HashSet<Integer> set1 = getNodeAncestors(g, nodeA);
					for (int j = i + 1; j < ds.length; j++) {
						int nodeB = ((Integer) ds[j]).intValue();
						// ��ȡnodeB�����������Ƚ��
						HashSet<Integer> set2 = getNodeAncestors(g, nodeB);
						for (Iterator<Integer> sIter = set1.iterator(); sIter.hasNext();) {
							Object n1 = sIter.next();

							/*
							 * ���nodeA��nodeB���������Ƚ��������ͬ��Ԫ�أ�
							 * ��˵������һ�����Ƚ�����ͨ�����������ߵ���nodeA��nodeB
							 */
							if (set2.contains(n1)) {
								// �����ת��Ϊ����
								if (g.getEdge(nodeA, nodeB) == Graph.NO_EDGE && g.getEdge(nodeB, nodeA) == Graph.NO_EDGE) {
									g.addEdge(nodeA, nodeB, SHARED_EDGE);
								} else if (g.getEdge(nodeA, nodeB) != Graph.NO_EDGE || g.getEdge(nodeB, nodeA) != Graph.NO_EDGE) {
									int nodeC = g.addNode(VIRTUAL_NODE--);
									g.addEdge(nodeA, nodeC, SHARED_EDGE);
									g.addEdge(nodeC, nodeB, SHARED_EDGE);
								}
								break;
							}
						}
					}
				}
			}
		}
		inputGraphCount++;
		return g;
	}

	/**
	 * ���ݸ�����ͼg�ͽ��node����ȡnode���������Ƚ�㣨node����Ҳ��Ϊ���������Ƚ�㣩
	 * 
	 * @param graph
	 * @param node
	 * @return
	 */
	private HashSet<Integer> getNodeAncestors(Graph graph, int node) {
		HashSet<Integer> set = new HashSet<Integer>();
		Stack<Integer> searcher = new Stack<Integer>();
		DirectedListGraph g = (DirectedListGraph) graph;
		set.add(new Integer(node)); // �Ƚ�node������ӵ����ȼ�����
		searcher.push(new Integer(node));
		while (!searcher.empty()) {
			int curnode = ((Integer) searcher.pop()).intValue();
			int incomingEdges[] = g.getIncomingNodeEdgeSet(curnode);
			for (int i = 0; i < incomingEdges.length; i++) {
				if (g.getEdgeLabel(incomingEdges[i]) == CTRL_EDGE || g.getEdgeLabel(incomingEdges[i]) =='C') {
					int incomingNode = g.getOtherNode(incomingEdges[i], curnode);
					// �жϵ�ǰ����Ƿ���set�г��֣�ֻ����û�г��ֵ�ʱ��Ž���ǰ��node��ջ����ֹPDGͼ�г��ֻ�ʱ������ѭ��
					if (!set.contains(new Integer(incomingNode)) ) {
						set.add(new Integer(incomingNode));
						searcher.push(new Integer(incomingNode));
					}
				}
			}
		}
		return set;
	}

	public Graph parse(String text, GraphFactory factory) throws ParseException {
		/* ��ʱû��ʵ�� */
		throw new UnsupportedOperationException("��֧�ֵĲ�����");
	}

	//������������ͼ�� 
	public Graph[] parse(InputStream in, GraphFactory factory) throws IOException, ParseException {
		
		inputGraphCount=0;
		
		SAXBuilder sax = new SAXBuilder();
		epdgGraphs.clear();
		try {
			Document pdgDoc = sax.build(in);
			Element root = pdgDoc.getRootElement(); // PDG
			List<?> classes = root.getChildren(); // Class
			Iterator<?> iter = classes.iterator();
			while (iter.hasNext()) {
				Element cls = (Element) iter.next();
				String className = cls.getAttributeValue(CLASS_NAME);
				List<?> methods = cls.getChildren(); // Method
				Iterator<?> mIter = methods.iterator();
				while (mIter.hasNext()) {
					Element method = (Element) mIter.next();
					DirectedListGraph g = parse(method, className, DirectedListGraph.Factory.instance);
					/*UndirectedListGraph graph = new UndirectedListGraph();
					graph = graph.convertToUndirectedListGraph(g);
					epdgGraphs.add(graph);*/
					epdgGraphs.add(g);
				}
			}
			return (Graph[]) epdgGraphs.toArray(new Graph[epdgGraphs.size()]);
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String serialize(Graph g) {
		if (g == null) return "";
		StringBuffer buf = new StringBuffer();
		
		buf.append("--------------------------------------------------------\n");
		
		if (g instanceof de.parmol.FFSM.Matrix) {
			buf.append("��ͼ��ţ�" + g.getName() + "\n");
		} else {
			buf.append("�������ƣ�" + g.getName() + "\n");
		}

		HashMap<Integer, String> statementMap = new HashMap<Integer, String>();
		statementMap = labelMap.get(g.getName());
		
		//ϵ�л��ڵ�
		for (int i = 0; i < g.getNodeCount(); i++) {
			//if (g.getNodeLabel(i) == VIRTUAL_NODE && g.getDegree(i)==1) return null;
			buf.append("����ţ�" + i);
			for (int k = 0; k < 5; k++)
				buf.append(' ');
			buf.append("����ǩ��" + g.getNodeLabel(i));
			for (int k = 0; k < 12; k++)
				buf.append(' ');
			
			//�˴�ʵ���ϴ������⡣��Ϊĳһ����ǩ��Ӧ����ͬһ�﷨�ṹ��������䡣�õ���statement��һ���Ǹýڵ��Ӧ����䣬��ֻ���﷨�ṹ��ͬ
			//������Embedding��ά��ÿһ���ڵ��Ӧ����䣬�����ڴ����Ļ�Ƚϴ�
			if (statementMap != null) {
				String statement = (String) statementMap.get((Integer)i);
				if (statement == null) {
					buf.append("��䣺(������)");
				} else {
					buf.append("��䣺" + statement);
				}
			} else {

				Set<String> statementSet = LabelStatementSet.get(String.valueOf(g.getNodeLabel(i)));
				if (statementSet == null) {
					buf.append("��䣺(������)");
				} else {
					buf.append("��䣺");
					int maxNum = 2;
					if (Settings.outputMoreStatement == true) maxNum = 9;
					int i3 = 1;
					for (String s: statementSet) {
						if (i3++>maxNum) break;
						buf.append("\n"+s);
					}
				}
			}
			
			buf.append("\n");

		}

		//ϵ�л���
		for (int i = 0; i < g.getEdgeCount(); i++) {
			final int edge = g.getEdge(i);
			buf.append("�߱�ţ�" + i);
			for (int k = 0; k < 5; k++)
				buf.append(' ');
			buf.append("�߱�ǩ��" + (char) g.getEdgeLabel(edge));
			for (int k = 0; k < 5; k++)
				buf.append(' ');

			buf.append("���A��" + g.getNodeA(edge));
			for (int k = 0; k < 5; k++)
				buf.append(' ');
			buf.append("���B��" + g.getNodeB(edge));
			for (int k = 0; k < 5; k++)
				buf.append(' ');

			if (g.getEdgeLabel(edge) == CTRL_EDGE || g.getEdgeLabel(edge) == 'C') {
				buf.append("�����ͣ�����������");
			} else if (g.getEdgeLabel(edge) == DATA_EDGE || g.getEdgeLabel(edge) == 'D') {
				buf.append("�����ͣ�����������");
			} else if (g.getEdgeLabel(edge) == SHARED_EDGE) {
				buf.append("�����ͣ���������������");
			}
			buf.append('\n');
		}

		//buf.append("***********************\n");
		return buf.toString();
	}

	public void serialize(Graph[] graphs, OutputStream out) throws IOException {
		BufferedOutputStream bout = new BufferedOutputStream(out);

		for (int i = 0; i < graphs.length; i++) {
			bout.write(serialize(graphs[i]).getBytes());
		}
		bout.flush();
	}

	/*
	 * ���������ϣ��ĳһ���ļ���
	 */
	public static void outputHashMap(HashMap<?,?> outHashMap, String outputFileName) throws IOException
	{
		FileOutputStream out = new FileOutputStream(new File(outputFileName));
		Iterator iter = outHashMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			out.write(key.toString().getBytes());
			out.write('\t');
			out.write(val.toString().getBytes());
			out.write('\n');
		}
	}
	
	
	public static void main(String[] argvs) throws Exception {
		Graph[] output; 
		
		FileInputStream in = new FileInputStream(new File("test.xml"));
		output = new PDGParser().parse(in, UndirectedListGraph.Factory.instance);
		
		instance.serialize(output, new FileOutputStream(new File("data\\ParserOutput.txt")) );
		
		outputHashMap(LabelStatementMap, "data\\LabelStatementMap.txt");
	}
}
