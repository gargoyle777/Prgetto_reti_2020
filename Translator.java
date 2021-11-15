package progetto_reti;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.locks.Lock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Translator implements Runnable{
	private Challenge cinfo;
	private int i=0;
	private String[] richieste= new String[6];
	private int random;
	private Random randomGen;
	private ArrayList<String> dictionary;		//se non sincronizzo potrei avere due sfide con le stesse parole
	private Lock lock;
	private int dictionaryDim;
	private JSONParser parser= new JSONParser();
	public Translator(Challenge cinfo,ArrayList<String> dictionary,Lock lock,Random random) {
		this.cinfo=cinfo;
		this.dictionary=dictionary;
		this.lock=lock;
		this.randomGen=random;	
		dictionaryDim=dictionary.size();
	}
	public void run() {
		String rawRisposta;
		JSONArray rawTraduzioni;
		Object objRisposta;
		ArrayList<ArrayList<String>> traduzioni=new ArrayList<ArrayList<String>>();
		int tmpC=0;
		lock.lock();
		random= randomGen.nextInt(dictionaryDim);
		Collections.shuffle(dictionary);
		for(int c=0;c<6;c++) {
			if(random==dictionaryDim)	random=0;
			richieste[c]=dictionary.get(random).toLowerCase();
			random++;
		}
		lock.unlock();
		while(i<6) {		
			try {
				URL url= new URL("https://api.mymemory.translated.net/get?q="+richieste[i]+"&langpair=it|eng");
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestMethod("GET");
	            BufferedReader in= new BufferedReader(new InputStreamReader(connection.getInputStream()),1000);
	            rawRisposta=in.readLine();
	            objRisposta= parser.parse(rawRisposta);
	            traduzioni.add(new ArrayList<String>());
	            	rawTraduzioni=(JSONArray) ((JSONObject) objRisposta).get("matches");	
	            	Iterator<JSONObject> iterator=rawTraduzioni.iterator();
	            	tmpC=0;
	            	while(iterator.hasNext() && tmpC<10) {
	            		traduzioni.get(i).add((String) (((JSONObject) iterator.next()).get("translation")));
	            		tmpC++;
		            }
	            i++;
			}
			catch(Exception e) {
				e.printStackTrace();
				cinfo.setupError();
			}
		}
		cinfo.setWords(richieste,traduzioni);		
	}
}
