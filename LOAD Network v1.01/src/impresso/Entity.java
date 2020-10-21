package impresso;

import org.json.JSONObject;

public class Entity extends Token {
	private String type;
	private String system_id;
	
	public Entity() {
		
	}
	public Entity(JSONObject entity, String language) {
		this.type = entity.getString("type");
		this.system_id = entity.getString("system_id");
		setLanguage(language);
		setOffset(entity.getInt("start_offset"));
		setLemma(entity.getString("name"));
		setSurface(entity.getString("surface"));
	}
	//super?

	public String getSystemId() {
		return system_id;
	}
	
	public String getType() {
		return type;
	}
	
}
