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
		this.type = entity.getString("type");
		this.system_id = entity.getString("system_id");
		try {
			this.entityId = entity.getString("entity_id");
		} catch(JSONException e) {
			this.entityId = "_" + entity.getString("name");
		}

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

	public String getEntityId() { return entityId; }
	
}
