package progetto_reti;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

public class Challenge{
	private long timeStart;
	private SelectionKey crkey,cdkey;
	String[] itWords;
	ArrayList<ArrayList<String>>	engWords;	//piu risultati
	private int crRA=0,cdRA=0,crWA=0,cdWA=0;			
	private int crCounter=0,cdCounter=0;
	public Challenge(SelectionKey crkey) {
		this.crkey=crkey;
	}
	
	public void challengeAccepted(SelectionKey cdkey) {
		this.cdkey=cdkey;
		timeStart=System.currentTimeMillis();
	}
	
	public void setupError() {
		((Attachment) crkey.attachment()).setOutput("-5:"+itWords[0]);
		((Attachment) cdkey.attachment()).setOutput("-5:"+itWords[0]);
		((Attachment) crkey.attachment()).attachedUser().unsetflagChallengeStatus();
		((Attachment) cdkey.attachment()).attachedUser().unsetflagChallengeStatus();
	}
	public void setWords(String[] itWords, ArrayList<ArrayList<String>> engWords) {
		this.itWords=itWords;
		this.engWords=engWords;
		((Attachment) crkey.attachment()).setOutput("-3:"+itWords[0]);
		((Attachment) cdkey.attachment()).setOutput("-3:"+itWords[0]);
		((Attachment) crkey.attachment()).attachedUser().setReady();
		((Attachment) cdkey.attachment()).attachedUser().setReady();
		}
	
	public void answer(Attachment attach, String answer) {
		answer=answer.trim();
		if(attach.attachedUser().equals(((Attachment)crkey.attachment()).attachedUser())) {
			if(checkTimeOver()) {
				crCounter=6;
			}
			try {
				int a=0;
				while(crCounter<6) {
					if((engWords.get(crCounter)).get(a).equalsIgnoreCase(answer.trim())) {
						crRA++;
						break;
					}
					a++;
				}
			}
			catch(IndexOutOfBoundsException e ){
				crWA++;
			}
			crCounter++;
			if(crCounter>=6) {
				if(cdCounter<6) {
					attach.setOutput("-1:aspetta il tuo avversario per il resoconto");
					((Attachment) crkey.attachment()).attachedUser().setWaitingOtherPlayer();
				}
				else {
					challengeEnd(attach);
					challengeEnd((Attachment) cdkey.attachment());
				}
			}
			else{
				attach.setOutput(itWords[crCounter]);
			}
		}
		else {
			if(checkTimeOver()) {
				cdCounter=6;
			}
			try {
				int a=0;
				while(cdCounter<6) {
					if(engWords.get(cdCounter).get(a).equalsIgnoreCase(answer.trim())) {
						cdRA++;
						break;
					}
					a++;
				}
			}
			catch(IndexOutOfBoundsException e ){
				cdWA++;
			}
			cdCounter++;
			if(cdCounter>=6) {
				if(crCounter<6) {
					attach.setOutput("-1:aspetta il tuo avversario per il resoconto");
					((Attachment) cdkey.attachment()).attachedUser().setWaitingOtherPlayer();
				}
				else {
					challengeEnd(attach);
					challengeEnd((Attachment) crkey.attachment());
				}
			}
			else	attach.setOutput(itWords[cdCounter]);
		}
	}
	
	public void challengeEnd(Attachment attach) {
		int crScore= (crRA*4)-crWA;
		int cdScore= (cdRA*4)-cdWA;
		if(attach.attachedUser().equals(((Attachment)crkey.attachment()).attachedUser())) {
			if(crScore>cdScore) {
				((Attachment) crkey.attachment()).attachedUser().updateScore(crScore+10);
				attach.setOutput("-2:"+crRA+" risposte corrette,"+"\n"
						+ crWA+" risposte sbagliate,"+"\n"
						+crScore+" punti."+"\n"
								+ "HAI VINTO!");
			}
			else if(crScore<cdScore) {
				((Attachment) crkey.attachment()).attachedUser().updateScore(crScore);
				attach.setOutput("-2:"+crRA+" risposte corrette,"+"\n"
						+ crWA+" risposte sbagliate,"+"\n"
								+crScore+" punti."+"\n"
										+ "HAI PERSO!");
			}
				else {
					((Attachment) crkey.attachment()).attachedUser().updateScore(crScore);
					attach.setOutput("-2:"+crRA+" risposte corrette,"+"\n"
							+ crWA+" risposte sbagliate,"+"\n"
									+crScore+" punti."+"\n"
											+ "PAREGGIO!");
				}
			((Attachment) crkey.attachment()).attachedUser().unsetflagChallengeStatus();
			((Attachment) crkey.attachment()).attachedUser().unregisterChallenge();
			}
		else {
			if(cdScore>crScore) {
				((Attachment) cdkey.attachment()).attachedUser().updateScore(cdScore+10);
				attach.setOutput("-2:"+cdRA+" risposte corrette,"+"\n"
						+ cdWA+" risposte sbagliate,"+"\n"
						+cdScore+" punti."+"\n"
								+ "HAI VINTO!");
			}
			else if(cdScore<crScore) {
				((Attachment) cdkey.attachment()).attachedUser().updateScore(cdScore);
				attach.setOutput("-2:"+cdRA+" risposte corrette,"+"\n"
						+ cdWA+" risposte sbagliate,"+"\n"
								+cdScore+" punti."+"\n"
										+ "HAI PERSO!");
			}
				else {
					((Attachment) cdkey.attachment()).attachedUser().updateScore(cdScore);
					attach.setOutput("-2:"+cdRA+" risposte corrette,"+"\n"
							+ cdWA+" risposte sbagliate,"+"\n"
									+cdScore+" punti."+"\n"
											+ "PAREGGIO!");
				}
			((Attachment) cdkey.attachment()).attachedUser().unsetflagChallengeStatus();
			((Attachment) cdkey.attachment()).attachedUser().unregisterChallenge();
		}
	}
	
	public boolean checkTimeOver() {
		if((System.currentTimeMillis()-timeStart)>60000) return true;
		else return false;
	}


}
