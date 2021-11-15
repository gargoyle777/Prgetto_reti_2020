package progetto_reti;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class User {
	private String challenging;
	private String name,password;
	private int score;
	private boolean logstatus=false;
	private int UDPPort;
	private int flagChallengeStatus=0;
	private long timestamp;
	private HashSet<String> beingChallengedBy= new HashSet<String>();		
	private Challenge ongoingChallenge;
	private ArrayList<String> friendlist= new ArrayList<String>();
	
	
	public User(JSONObject jsonUser){
		name=(String) jsonUser.get("name");
		password=(String) jsonUser.get("password");
		score=(int)(long) jsonUser.get("score");
		JSONArray rawfriendlist= (JSONArray)jsonUser.get("friendlist");
		Iterator<String> iterator= rawfriendlist.iterator();
		while(iterator.hasNext()) {
			friendlist.add(iterator.next());
		}
	}
	
	public User(String username, String psw) {
		name=username;
		password=psw;
	}
	
	public void setLogStatus() {
		logstatus=true;
	}

	public String getName(){
		return name;
	}
	
	public int getScore(){
		return score;
	}
	
	public void updateScore(int update) {
		score=score+update;
	}

	public boolean checkPSW(String psw) {
		if(password.equals(psw)) return true;
		else return false;
	}
	
	public boolean checkstatus() {
		return logstatus;
	}

	public boolean addFriend(String name) {
		if(friendlist.contains(name))	return false;
		friendlist.add(name);
		return true;
	}
	
	public ArrayList<String> getFriendList() {
		return friendlist;
	}
	
	public JSONObject getJSON() {
		JSONObject juser=new JSONObject();
		juser.put("name",name);
		juser.put("password", password);
		juser.put("score", new Integer(score));
		JSONArray jfriendlist=new JSONArray();
		for(int i=0;i<friendlist.size();i++) {
			jfriendlist.add(friendlist.get(i));
		}
		juser.put("friendlist",jfriendlist);
		return juser;
	}
	
	public String getFriendListJSON() {
		StringBuilder sbuilder= new StringBuilder();
		Iterator<String> iterator=friendlist.iterator();
		sbuilder.append("{"
				+ "\"friendlist\": [");
		while(iterator.hasNext()) {
			sbuilder.append("\""+iterator.next()+"\"");
			if(iterator.hasNext())	sbuilder.append(", ");
		}
		sbuilder.append("]"
				+ "}");
		return sbuilder.toString();
	}

	public boolean checkFriend(String friend) {
		if(friendlist.contains(friend))	return true;
		else	return false;
	}
	
	public void setWaiting() {
		flagChallengeStatus=1;
	}
	
	public void setWaitingTranslator() {
		flagChallengeStatus=5;
	}
	
	public boolean isWaitingTranslator() {
		return flagChallengeStatus==5;
	}
	
	public void createChallenge(String challenged,SelectionKey key) {
		challenging=challenged;
		setWaiting();
		ongoingChallenge=new Challenge(key);
		timestamp=System.currentTimeMillis();
	}
	
	public String whoamichallenging() {
		return challenging;
	}
	public void unsetflagChallengeStatus() {
		flagChallengeStatus=0;
		timestamp=0;
		ongoingChallenge=null;
	}
	
	public boolean isNotWaiting() {
		return flagChallengeStatus==0;
	}
	
	public boolean isWaiting() {
		return flagChallengeStatus==1;
	}

	public boolean isDeclined() {
		return flagChallengeStatus==2;
	}

	public boolean checkTimeChallenge() {
		if((System.currentTimeMillis()-timestamp)>15000){
			flagChallengeStatus=0;
			timestamp=0;
			return false;
		}
		return true;
	}

	public void setUDPPort(int udpport){
		UDPPort=udpport;
	}
	
	public int getUDPPort() {
		return UDPPort;
	}
	
	public boolean equals(String name) {		//nickname unici, sovrascritto equals
		return name.equals(this.name);
	}
	
	public boolean equals(User user) {		//nickname unici, sovrascritto equals
		return user.getName().equals(name);
	}

	public void addChallenger(String name) {
		beingChallengedBy.add(name);
	}

	public boolean checkChalleger(String name) {
		if(beingChallengedBy.contains(name))	return true;
		else return false;
	}

	public void logout() {
		logstatus=false;
		UDPPort=-1;
		flagChallengeStatus=0;
		beingChallengedBy=null;
	}
	
	public void acceptedChallenge(SelectionKey key) {
		beingChallengedBy.removeAll(beingChallengedBy);
		ongoingChallenge.challengeAccepted(key);
	}
	
	public void declinedChallenge() {
		flagChallengeStatus=2;
	}

	public boolean isChallenging() {
		return flagChallengeStatus==4;
	}

	public void setReady() {
		flagChallengeStatus=4;
	}
	
	public void removeChallenger(String name) {
		beingChallengedBy.remove(name);
	}

	public void registerChallenge(Challenge ongoingChallenge) {
		this.ongoingChallenge=ongoingChallenge;
	}
	
	public void unregisterChallenge() {
		ongoingChallenge=null;
	}
	public Challenge getOngoingChallenge() {
		return ongoingChallenge;
	}

	public void setWaitingOtherPlayer() {
		flagChallengeStatus=6;
	}

	public boolean isWaitingOtherPlayer() {
		return flagChallengeStatus==6;
	}
	
	public void setWaitingTimeend() {
		flagChallengeStatus=3;
	}
	
	public boolean isWaitingTimeend() {
		return flagChallengeStatus==3;
	}
}

