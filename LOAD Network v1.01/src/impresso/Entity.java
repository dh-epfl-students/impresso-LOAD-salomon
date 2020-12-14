package impresso;

import org.json.JSONException;
import org.json.JSONObject;

public class Entity extends Token {
	private String type;
	private String system_id;
	private String entityId;

	public Entity() {
		
	}
	public Entity(JSONObject entity, String language) {
		this.type = entity.getString("entity_type");
		this.system_id = entity.getString("system_id");
		this.entityId = entity.getString("entity_id");

		setLanguage(language);
		setOffset(entity.getInt("start_offset"));
		setLemma(entity.getString("mention"));
		setSurface(entity.getString("surface"));
	}

	public String getSystemId() {
		return system_id;
	}
	
	public String getType() {
		return type;
	}

	public String getEntityId() { return entityId; }
	
}
