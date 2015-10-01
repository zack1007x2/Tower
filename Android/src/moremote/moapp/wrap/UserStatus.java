package moremote.moapp.wrap;

/**
 * Created by lintzuhsiu on 14/11/5.
 */
public class UserStatus {

    private String username;
    private String type;
    private String status;
    private String subDescription;
    
    public UserStatus() {
        type = "unavailable";
    }

    public void setSubDescription(String subDescription) {
        this.subDescription = subDescription;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public String getSubDescription() {
        return subDescription;
    }

    public String getType() {
        return type;
    }
    
    public String getUsername() {
    	return username;
    }
	
}
