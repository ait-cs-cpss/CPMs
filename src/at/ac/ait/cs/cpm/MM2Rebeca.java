/**
 * Author: Stefan Marksteiner (c) 2026 by the AIT Austrian Institute of Technology GmbH
 * Licensed under the EUPL
 * This class provides a converter from Mealy Machines in GraphViz (.dot) format as written by the 
 * LearnLib automata learning framework to the Rebeca modeling language where it can be model-checked.
 * The Mealy will be augmented with propositions using CPMs following the workflow
 * Mealy => AMM => AMMτ => Rebeca
 *  and intermediate products (AMMs, AMMs with τ states) can be saved and loaded.
 */
package at.ac.ait.cs.cpm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class MM2Rebeca {
	
	
	
	public static void main(String[] args)  {
		MM2Rebeca converter = new MM2Rebeca();

		
		//TODO GetCPMfromJSONFile		
		MM2Rebeca.CPM cpm = converter.new CPM();
		cpm.addGain(converter.new Condition("PROT","$INIT","*"));
		cpm.addGain(converter.new Condition("AUTH","CRED","OK"));
		cpm.addLose(converter.new Condition("AUTH","CRED","ERR"));
		cpm.addInternal(converter.new Condition("ACCESSOK","READ","OK"));
		converter.MM2AMM("MM_example.dot",cpm);
		converter.writeAMMDot("AMM_example.dot");
		converter.AMM2AMMT(cpm);
		converter.writeAMMTDot("AMM_example_w_Tau.dot");
		converter.writeRebeca("Rebeca_Example");
	}
	
	
	public class Condition {
		String proposition = "";
		String input = "";
		String output = "";
		
		Condition(String proposition, String input, String output){
			this.proposition=proposition;
			this.input=input;
			this.output=output;
		}
		
		public String toString() {
			return "["+proposition+":"+input+"/"+output+"]";
		}
		
		public boolean equals(Object o) {
			if(o.getClass()!=Condition.class)			
				return false;
			if(((Condition)o).input.equals(input) &&
			   ((Condition)o).output.equals(output) &&
			   ((Condition)o).proposition.equals( proposition))
					return true;
			return false;							
		}
		
		boolean match (Transition t) {
			if((t.input.trim().toLowerCase().equals(input.trim().toLowerCase()) ||
					input.trim().equals("*"))
					&&
			  ( t.output.trim().toLowerCase().equals(output.trim().toLowerCase()) ||
					 input.trim().equals("*")))
		
				
				return true;
			return false;
		}
		
		String getProposition (Transition t) {
			if(match(t))
				return proposition;
			return "";
		}
	}
	
	public class CPM {
		List<Condition> gain = new ArrayList<Condition>();
		List<Condition> lose = new ArrayList<Condition>();
		List<Condition> internal = new ArrayList<Condition>();
		
		void addGain(Condition c) {
			gain.add(c);
		}
		
		void addLose(Condition c) {
			lose.add(c);
		}
		
		void addInternal(Condition c) {
			internal.add(c);
		}
		
		List<Condition> getGainConditions(){
			return gain;
		}
		
		List<Condition> getLoseConditions(){
			return lose;
		}
		
		List<Condition> getInternalConditions(){
			return internal;
		}
		
		List<String> getGains(Transition t){
			List<String> result = new ArrayList<String>();
			for(Condition c:gain)
				if(c.match(t))
					result.add(c.proposition);			
			return result;
			
		}
		
		List<String> getInitialGains(){
			List<String> result = new ArrayList<String>();
			for(Condition c:gain)
				if(c.match(new Transition("","","$INIT","*")))
					result.add(c.proposition);			
			return result;
		}
		
		List<String> getLosses(Transition t){
			List<String> result = new ArrayList<String>();
			for(Condition c:lose)
				if(c.match(t))
					result.add(c.proposition);			
			return result;
		}
		
		List<String> getInternals(Transition t){
			List<String> result = new ArrayList<String>();
			for(Condition c:internal)
				if(c.match(t))
					result.add(c.proposition);			
			return result;
		}
		
	}
	
	public class State {
		String name="";
		int id=-1;
				
		State(int id,String name){
			this.name=name;
			this.id=id;
		}
		
		public String toString() {
			return "("+id+","+name+")";
		}
		
	}
	
	public class ExtendedState extends State {
		HashSet<String> propositions= new HashSet<String>();
		
		ExtendedState(int id,String name){
			super(id,name);
		}
		
		void addProposition(String proposition){
			propositions.add(proposition);
		}
		
		void removeProposition(String proposition){
			propositions.remove(proposition);
		}
		
		HashSet<String> getPropositions(){
			return propositions;
		}
		
		@Override
		public String toString() {
			return "("+id+","+name+","+propositions+")";
		}
	}
	
	public class Transition {
		String source="", target="", input="", output="";
		
		Transition(Transition t){
			this.source=t.source;
			this.target=t.target;
			this.input=t.input;
			this.output=t.output;				
		}
		
		Transition(String source, String target, String input, String output){
			this.source=source;
			this.target=target;
			this.input=input;
			this.output=output;			
		}
		
		public String toString() {
			return source+"-"+input+"/"+output+"->"+target;
		}
		
		//A Mealy can only have one transition per state and Input Symbol
		//This also applies for AMMs (with and without τ states)
		public boolean equals(Object o) {
			if(o.getClass()==Transition.class)
				if( ((Transition)o).source.equals(source) && ((Transition)o).input.equals(input) )
					return true;
			return false;
		}
	}
	
	public class ExtendedTransition extends Transition{
		HashSet<String> propositions= new HashSet<String>();
		
		ExtendedTransition(Transition t){
			super(t);
		}
		
		ExtendedTransition(String source, String target, String input, String output){
			super(source,target,input,output);		
		}
		
		void addProposition(String proposition){
			propositions.add(proposition);
		}
		
		void removeProposition(String proposition){
			propositions.remove(proposition);
		}
		
		void setProposition(HashSet<String> propositions) {
			this.propositions=propositions;
		}
		
		HashSet<String> getPropositions(){
			return propositions;
		}
		
		public String toString() {
			String ret =  source+"\"-"+input+"/"+output+"->"+target+propositions+"; ";
			return ret;
		}
	}
	
	private HashMap<Integer,ExtendedState> states = new HashMap<>();
	private HashSet<Transition> transitions = new HashSet<>();
	private int initialStateId=-1;
	private HashSet<String> propositions = new HashSet<String>();
	private HashSet<String> inputs = new HashSet<String>();
	private HashSet<String> outputs = new HashSet<String>();
	private HashSet<String> implicitProps = new HashSet<String>();

	
	
	private void init() {
		states = new HashMap<>();
		transitions = new HashSet<>();
		initialStateId=-1;
		propositions = new HashSet<String>();
		inputs = new HashSet<String>();
		outputs = new HashSet<String>();
		implicitProps = new HashSet<String>();
	}
	
	void MM2Rebeca(String MMFilename, CPM cpm, String RebecaFilename) {
		MM2AMM(MMFilename,cpm);
		AMM2Rebeca(cpm, RebecaFilename);		
	}
	
	void AMM2Rebeca(CPM cpm, String RebecaFilename)  {
		AMM2AMMT(cpm);
		writeRebeca(RebecaFilename);
	}
	
	void MM2AMM(String MMFilename, CPM cpm) {
		init();
		HashMap<Integer,ExtendedState> mStates = new HashMap<Integer,ExtendedState>();
		HashSet<Transition> mTransitions = new HashSet<Transition>();
		int initialState=0;
		 
		List<String> lines = new ArrayList<String>();
		try {
			lines= Files.readAllLines(Paths.get(MMFilename), Charset.defaultCharset());   
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		for(String line:lines) {
			if(line.contains("[")&& !line.contains("__start") && !line.contains("->")) {
				String[] name_props=line.split("label=\"")[1].split("\"")[0].split("\\\\n");
				int number = Integer.parseInt(line.split("\\[")[0].trim().replaceAll("[\\D]", ""));
				ExtendedState state = new ExtendedState(number,name_props[0].trim());				
			
				mStates.put(state.id,state);
			}
			if(line.contains("__start0 -> "))
				initialState=Integer.parseInt(line.replaceAll("[\\D]", ""));
			else if(line.contains("->")) {
				mTransitions.add(new Transition (
						line.split("->")[0].trim(),
						line.split("->")[1].split("\\[")[0].trim(),
						line.split("label=\"")[1].split("/")[0].trim(),
						line.split("label=\"")[1].split("/")[1].split("\"")[0].trim()
						));
				inputs.add(line.split("label=\"")[1].split("/")[0].trim());
				outputs.add(line.split("label=\"")[1].split("/")[1].split("\"")[0].trim());
			}
			
		}
		
		HashSet<String >mPropositions = new HashSet<String>();
		for(Transition t:mTransitions) {
			
			for(String gain:cpm.getGains(t)) {
				mStates.get(Integer.parseInt(t.target.trim().replaceAll("[\\D]", ""))).addProposition(gain);
				mPropositions.add(gain);
			}
				
			
		}
		for(String init:cpm.getInitialGains()) {
			mStates.get(initialState).addProposition(init);
			mPropositions.add(init);
		}
		
		//PROPAGATE PROPOSITIONS
		boolean changed=false;
		while(!changed) {
			changed=false;
			for(Transition t:mTransitions) {				
				List<String> losses = cpm.getLosses(t);
				HashSet<String> sourceProps = new HashSet<String>(mStates.get(Integer.parseInt(t.source.trim().replaceAll("[\\D]", ""))).getPropositions());
				sourceProps.removeAll(losses);
				for(String prop:sourceProps)
					mStates.get(Integer.parseInt(t.target.trim().replaceAll("[\\D]", ""))).addProposition(prop);
				if(!sourceProps.isEmpty())
					changed=true;
			}
		}
		
		states=mStates;
		transitions=mTransitions;
		initialStateId=initialState;
		propositions=mPropositions;
		
		
		
	}
	
	
	void AMM2AMMT(CPM cpm) {
		if(states.isEmpty() || transitions.isEmpty() || initialStateId==-1 || propositions.isEmpty())
			try {
				throw new Exception("No AMM loaded - load AMM before transferring to AMMT.");					
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		HashSet<Transition> tempTrans = transitions;
		transitions = new HashSet<Transition>();
		for(Transition t:tempTrans) {
			ExtendedTransition extT = new ExtendedTransition(t);
			for(String g:cpm.getGains(t)) 
					extT.addProposition("+"+g);
			
			for(String l:cpm.getLosses(t)) 
				extT.addProposition("-"+l);
			
			for(String i:cpm.getInternals(t)) { 
				extT.addProposition("+"+i);
				implicitProps.add(i);
				propositions.add(i);
			}
			transitions.add(extT);
		}
		
	}
	
	

	void writeAMMDot(String filename) {		
		if(states.isEmpty() || transitions.isEmpty() || initialStateId==-1 || propositions.isEmpty())
			try {
				throw new Exception("No AMM loaded - load model before transferring to Rebeca.");		
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		
		PrintWriter writer = null;
    	try {
			writer = new PrintWriter(new FileWriter(new File(filename.contains(".dot")?filename:filename+".dot")));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    	
    	writer.println("digraph g {");
    	for(ExtendedState state:new HashSet<>(states.values())) {
    		writer.print("\t"+state.name+" [shape=\"circle\" label=\""+state.name);
    		for(String proposition:state.propositions) 
    			writer.print("\\n "+proposition.toUpperCase());
    		writer.println("\"];");    		
    	}
    	for(Transition t:transitions) {
    		writer.println("\t"+t.source.toUpperCase()+" -> "+t.target.toUpperCase()+" [label=\""+t.input.toUpperCase()+" / "+t.output.toUpperCase()+"\" ];");
    	}
    	writer.println("__start0 [label=\"\" shape=\"none\" width=\"0\" height=\"0\"];");
    	writer.println("__start0 -> "+states.get(initialStateId).name+";");
    	writer.println("}");
    	writer.flush();
    	
	}
	
	void writeAMMTDot(String filename) {		
		try {
			if(states.isEmpty() || transitions.isEmpty() || initialStateId==-1 || propositions.isEmpty())
				throw new Exception("No AMM loaded - load model before transferring to Rebeca.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		PrintWriter writer = null;
    	try {
			writer = new PrintWriter(new FileWriter(new File(filename.contains(".dot")?filename:filename+".dot")));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    	
    	writer.println("digraph g {");
    	for(ExtendedState state:new HashSet<>(states.values())) {
    		writer.print("\t"+state.name+" [shape=\"circle\" label=\""+state.name);
    		for(String proposition:state.propositions) 
    			writer.print("\\n "+proposition.toUpperCase());
    		writer.println("\"];");    		
    	}
    	int tauCnt=0;
    	for(Transition t:transitions) {
    		ExtendedTransition et = (ExtendedTransition)t;
    		HashSet<String> tauprops = new HashSet<String>(states.get(Integer.parseInt(t.source.replaceAll("[\\D]", ""))).propositions);
    		for(String prop:et.propositions) 
    			if(prop.startsWith("+"))
    				if(implicitProps.contains(prop.substring(1)))
    					tauprops.add(prop.substring(1));
    			
    	
    		writer.print("\tT"+tauCnt+" [shape=\"circle\" label=\"τ");
    		for(String proposition:tauprops) 
    			writer.print("\\n "+proposition.toUpperCase());
    		writer.println("\", style=dashed];");   
    		writer.println("\t"+et.source.toUpperCase()+" -> "+"T"+tauCnt+" [label=\""+et.input.toUpperCase()+" / "+et.output.toUpperCase()+"\" ];");
    		writer.println("\tT"+tauCnt+" -> "+et.target.toUpperCase()+" [label=\"ε / ε\" ];");
    		tauCnt++;
    	}
    	writer.println("__start0 [label=\"\" shape=\"none\" width=\"0\" height=\"0\"];");
    	writer.println("__start0 -> "+states.get(initialStateId).name+";");
    	writer.println("}");
    	writer.flush();
	}
	
	//Reads AMM (i.e., Mealy Machines augmented with propositions) from DOT files
	//To generate a Rebeca model, still needs a CPM for implicit state propositions, 
	//creating an intermediate AMMτ (i.e., τ-state-augmented AMMs)
	void readAMMDot(String dotFilename){
		init();
		List<String> lines = new ArrayList<String>();
		try {
			lines= Files.readAllLines(Paths.get(dotFilename), Charset.defaultCharset());   
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		for(String line:lines) {
			if(line.contains("[")&& !line.contains("__start") && !line.contains("->")) {
				String[] name_props=line.split("label=\"")[1].split("\"")[0].split("\\\\n");
				int number = Integer.parseInt(line.split("\\[")[0].replaceAll("[\\D]", ""));
				ExtendedState state = new ExtendedState(number,name_props[0].trim());				
				for(int i=1;i<name_props.length;i++) {
					state.addProposition(name_props[i].trim());
					propositions.add(name_props[i].trim());
				}
				states.put(state.id,state);
			}
			if(line.contains("__start0 -> "))
				initialStateId=Integer.parseInt(line.replaceAll("[\\D]", ""));
			else if(line.contains("->")) {
				transitions.add(new Transition (
						line.split("->")[0].trim(),
						line.split("->")[1].split("\\[")[0].trim(),
						line.split("label=\"")[1].split("/")[0].trim(),
						line.split("label=\"")[1].split("/")[1].split("\"")[0].trim()
						));
				inputs.add(line.split("label=\"")[1].split("/")[0].trim());
				outputs.add(line.split("label=\"")[1].split("/")[1].split("\"")[0].trim());
			}
		}	
		
	}
	
	//Reads AMMτ (i.e., τ-state-augmented AMMs) from DOT files, 
	//which contains everything needed to generate a Rebeca file
	void readAMMTDot(String dotFilename){
		
		final int source = 0;
		final int target = 1;
		final int input  = 2;
		final int output = 3;
		
		init();
		
		List<String> lines = new ArrayList<String>();
		try {
			lines= Files.readAllLines(Paths.get(dotFilename), Charset.defaultCharset());   
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		HashSet<ExtendedState> tstates = new HashSet<ExtendedState>(); //Tau states
		HashMap<String,String[]> ptransitions = new HashMap<String,String[]>();
		HashSet<String> onlyExplicitProps = new HashSet<String>(); 		
		for(String line:lines) {
			if(line.contains("[")&& !line.contains("__start") && !line.contains("->")) {
				String[] name_props=line.split("label=\"")[1].split("\"")[0].split("\\\\n");
				String stateID = line.split("\\[")[0].trim();
				int number = Integer.parseInt(stateID.replaceAll("[\\D]", ""));
				ExtendedState state = new ExtendedState(number,name_props[0].trim());				
				for(int i=1;i<name_props.length;i++) {
					state.addProposition(name_props[i].trim());
					propositions.add(name_props[i].trim());
					if(stateID.matches("^S.*"))
						onlyExplicitProps.add(name_props[i].trim());
				}
				try {
				if(stateID.matches("^S.*"))
					states.put(state.id,state);
				else if(stateID.matches("^T.*"))
					tstates.add(state);					
				else
					throw new Exception("Could not read states - Must start with \"S\" and τ states with \"T\"");
				} catch(Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			if(line.contains("__start0 -> "))
				initialStateId=Integer.parseInt(line.replaceAll("[\\D]", ""));
			else if(line.contains("->")) {
				String p[]= {
						line.split("->")[0].trim(),
						line.split("->")[1].split("\\[")[0].trim(),
						line.split("label=\"")[1].split("/")[0].trim(),
						line.split("label=\"")[1].split("/")[1].split("\"")[0].trim()
				};
				String key="";
				try {
				if(p[source].matches("^T.*")) {
					key=p[source];
					p[source]=p[input]=p[output]="";					
				} else if(p[target].matches("^T.*")) {
					key=p[target];
					p[target]="";
					
				} else
					throw new Exception("Transition error: one end of a transition of an AMM with τ states must be a τ state (starting with \"T\").");
				} catch(Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
				String[] retrievedPTrans = ptransitions.get(key);
				if(retrievedPTrans==null)
					ptransitions.put(key, p);
				else
				{
					for(int i=0;i<3;i++) {
						if(retrievedPTrans[i].equals(""))
							retrievedPTrans[i]=p[i];
						
					}
				}
				
				
				String entry=line.split("label=\"")[1].split("/")[0].trim();
				if(!entry.equals("ε")) inputs.add(entry);
				entry=line.split("label=\"")[1].split("/")[1].split("\"")[0].trim();
				if(!entry.equals("ε")) outputs.add(entry);
			}
		}
		
		
		for(String p:propositions) 
			if(!onlyExplicitProps.contains(p))
				implicitProps.add(p);
		for(ExtendedState s:tstates) {
			String[] pTrans = ptransitions.get("T"+s.id);
			ExtendedTransition trans = new ExtendedTransition(pTrans[source],pTrans[target],pTrans[input],pTrans[output]);
			
			int x = Integer.parseInt(pTrans[source].replaceAll("[\\D]", ""));
			HashSet<String> srcProps = new HashSet<String>(states.get(x).getPropositions());
			x = Integer.parseInt(pTrans[target].replaceAll("[\\D]", ""));
			HashSet<String> tgtProps = new HashSet<String>(states.get(x).getPropositions());
			
			HashSet<String> tauProps = new HashSet<String>(s.propositions);
			tauProps.removeAll(srcProps);
			
			tgtProps.addAll(tauProps); //adding Propsitions from Tau state to include temporary propositions
			HashSet<String> plusProps = new HashSet<String>(tgtProps);
			plusProps.removeAll(srcProps);
			HashSet<String> minusProps = new HashSet<String>(srcProps);
			minusProps.removeAll(tgtProps);
			HashSet<String> diffProps = new HashSet<String>();
			for(String plus:plusProps)
				diffProps.add("+"+plus);
			for(String minus:minusProps)
				diffProps.add("-"+minus);
			trans.setProposition(diffProps);
			transitions.add(trans);
		}
		
		
		
		
	}
	
	
	void writeRebeca(String filename) {
		
		try {
			if(states.isEmpty() || transitions.isEmpty() || initialStateId==-1 || propositions.isEmpty())
				throw new Exception("No AMM loaded - load model before transferring to Rebeca.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		PrintWriter writer = null;
    	try {
			writer = new PrintWriter(new FileWriter(new File(filename.contains(".rebeca")?filename:filename+".rebeca")));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    	writer.println("reactiveclass ENVIRONMENT(3) {");
    	writer.println("\tknownrebecs {");
    	writer.println("\t\tSYSTEM system;");
    	writer.println("\t}");
    	writer.println("\tENVIRONMENT() { self.start(); }");
    	writer.println("\tvoid start() { system.req(); }");
    	for(String output:outputs)
    		writer.println("\tmsgsrv "+output.toLowerCase()+"(){ system.req(); }");
    	writer.println("}");
    	writer.println();
    	writer.println("reactiveclass SYSTEM(3) {");
    	writer.println("\tknownrebecs {");
    	writer.println("\t\tENVIRONMENT environment;");
    	writer.println("\t}");
    	writer.println("\tstatevars {");
    	if(implicitProps.size()<propositions.size()) {
    	writer.print("\t\tboolean ");
	    	for(Iterator<String> iterator=propositions.iterator();iterator.hasNext();) {
	    		String p = iterator.next();
	    		if(!implicitProps.contains(p)) {
	    			writer.print(p.trim().toLowerCase());
		    		if(iterator.hasNext()) writer.print(", ");
		    		else writer.println("; //Explicit state variables");
	    		}
	    	} 
    	}
    	writer.println("\t\tint state; //State tracker variable");
    	writer.print("\t\tboolean ");
    	for(String p:implicitProps)
    		writer.print(p.trim().toLowerCase()+", ");
    	writer.println("SYS_ERROR; //Implicit state properties + error flag");
    	writer.println("\t}");
    	writer.println("\tSYSTEM() {");
    	ExtendedState init = states.get(initialStateId);
    	for(String p:propositions) {
    		writer.println("\t\t"+p.trim().toLowerCase()+(init.propositions.contains(p)?"=true;":"=false;"));
    	}
    	writer.println("\t\tSYS_ERROR=false;");
    	writer.println("\t\tstate="+initialStateId+";");
    	writer.println("\t}");
    	writer.println("\tmsgsrv req() {");
    	writer.println("\t\tSYS_ERROR=false;");
    	for(String p:implicitProps)
    		writer.println("\t\t"+p.trim().toLowerCase()+"=false;");
    	
    	
    	writer.print("\t\tint data =?(");
    	for(int i=0;i<inputs.size();i++) {
    			writer.print(i);
    			if(i==inputs.size()-1)
    				writer.println(");");
    			else
    				writer.print(",");
    	}
    	writer.println("\t\tswitch(data) {");
    	int cid=0;
    	for(String input:inputs) {
    		writer.println("\t\t\tcase "+cid+": self."+input.toLowerCase().trim()+"(); break;");
    		cid++;
    	}    		
    	writer.println("\t\t\tdefault: self.err();");
    	writer.println("\t\t}");
    	writer.println("\t}");
    	writer.println("\tmsgsrv err(){");
    	writer.println("\t\tSYS_ERROR = true; //If we reach this, something is wrong.");
    	writer.println("\t}");
    	
    	
    	HashMap<String, HashMap<String,List<String>>> sysServers = new HashMap<String, HashMap<String,List<String>>>();
    	
    	//Resort Transitions to structure by input/source/target/output, because needed by the Rebeca control flow:
    	//msgsrv named input, if (state=source), set state to target, call output
    	for(Transition t:transitions) {
    		HashMap<String,List<String>> input = sysServers.get(t.input);
    		if(input==null) {
    			input = new HashMap<String,List<String>>();
    			sysServers.put(t.input, input);
    		}
    		List<String> vals = new ArrayList<>(Arrays.asList(new String[]{t.target,t.output}));
    		if(t.getClass()==ExtendedTransition.class)
    			for(String p:((ExtendedTransition)t).propositions)
    				vals.add(p);
    		//Check if cpm!=null when doing this and throw exception
    		//if (cpm==null && implicitProps.isEmpty())
    		input.put(t.source, vals);
    	}
    	
    	
    	for(Entry<String, HashMap<String, List<String>>> sysServer :sysServers.entrySet()) {
	    	writer.println("\tmsgsrv "+sysServer.getKey().toLowerCase().trim()+"(){");
	    	
	    	    	
	    	
	    	for(Iterator<Entry<String,List<String>>> conditionalit = sysServer.getValue().entrySet().iterator();conditionalit.hasNext();) {
	    	//for(Entry<String,List<String>> conditional:sysServer.getValue().entrySet()) {
	    		
	    		
	    		Entry<String,List<String>> conditional=conditionalit.next();
		    	writer.println("\t\tif(state=="+conditional.getKey().replaceAll("[\\D]", "")+") {");
		    	
		    	//1,2 are target state and output; additional values are proposition changes
		    	for(int i=2;i<conditional.getValue().size();i++) {
		    		String p=conditional.getValue().get(i);
		    		writer.print("\t\t\t"+p.substring(1).trim().toLowerCase()+" = ");
		    		if(p.substring(0, 1).equals("+"))
		    			writer.println("true;");
		    		if(p.substring(0, 1).equals("-"))
		    			writer.println("false;");
		    	}
		    	
		    	writer.println("\t\t\tstate="+conditional.getValue().get(0).replaceAll("[\\D]", "")+"; ");
		    	writer.println("\t\t\tenvironment."+conditional.getValue().get(1).toLowerCase().trim()+"();");		    	
		    	if(conditionalit.hasNext())
		    		writer.println("\t\t} else");
		    	else
		    		writer.println("\t\t}");		    	
	    	}
	    	writer.println("\t}");
    	}
    	writer.println("}");
    	writer.println();
    	writer.println("main {");
    	writer.println("\tENVIRONMENT environment(system):();");
    	writer.println("\tSYSTEM system(environment):();");
    	writer.println("}");
    	writer.flush();
    	
    	//WRITE REBECA property file
    	try {
			writer = new PrintWriter(new FileWriter(new File(filename.contains(".property")?filename:filename+".property")));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
    	writer.println("property {");
    	writer.println("\tdefine {");
		writer.println("\tSYS_ERROR = system.SYS_ERROR;");
		for(String p:propositions)
			writer.println("\t"+p.toUpperCase()+" = system."+p+";");
		for(int i=0;i<states.size();i++)
			writer.println("\tS"+i+" = (system.state == "+i+");");
		for(String p:implicitProps)
			writer.println("\t"+p.toUpperCase()+" = system."+p+";");		
		writer.println("\t}");
		writer.println("\tAssertion{");
		writer.println("\t\tAssertion0: (!SYS_ERROR);");
		writer.println("\t}");
		
		//These the are generic properties from the paper:
		//authentication, confidentiality, privilege levels, key validity
		//if propositions are undefined, the respective property should be commented out.
		//We don't want to set it with a dummy, because this could give the wrong impressions
		//the property would be satisfied.
		//TODO: Load properties dynamically
		writer.println("\tLTL{");
		writer.println("\t\tP1:G( (!(!AUTH &&PROT)) || !ACCESSOK);");
		writer.println("\t\tP2:G( (!PROT) || (!UREADOK));");
		writer.println("\t\tP3:G( (!PRIV || AUTH) && ((!(!PRIV &&CRIT)) || !ACCESSOK));");
		writer.println("\t\tP4:G(!INVKEYOK);");
		writer.println("\t}");
    	writer.println("}");
    	writer.println();
		writer.flush();
	}

}
