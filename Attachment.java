package progetto_reti;

import java.nio.ByteBuffer;

public class Attachment {
	private ByteBuffer output;
	private User user;
	public Attachment(String notloggedresponse) {
		output=ByteBuffer.wrap(notloggedresponse.getBytes());
	}
	
	public Attachment(User user, String output) {
		this.user=user;
		this.output=ByteBuffer.wrap(output.getBytes());
		
	}
	public synchronized void setOutput(String bb) {
		output=ByteBuffer.wrap(bb.getBytes());
	}
	
	public ByteBuffer getOutput() {
		return output;
	}
	
	public User attachedUser() {
		return user;
	}
	
	public void userloggedoff() {
		user.logout();
		user=null;
	}
}
