package latmod.lib.github;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;

import latmod.lib.util.FinalIDObject;

/**
 * Created by LatvianModder on 16.03.2016.
 */
public class GitHubRepo extends FinalIDObject {

    public final String owner;
    public final String repo;
    private Map<String, GitHubBranch> branches;

    public GitHubRepo(String u, String r) {
        super(u + '/' + r);
        owner = u;
        repo = r;
    }

    public Map<String, GitHubBranch> getBranches() throws Exception {
        if (branches == null) {
            Map<String, GitHubBranch> map = new LinkedHashMap<>();

            for (JsonElement e : GitHubAPI.getAPI("repos/" + getID() + "/branches").getAsJsonArray()) {
                GitHubBranch b = new GitHubBranch(this, e.getAsJsonObject());
                map.put(b.getID(), b);
            }

            branches = Collections.unmodifiableMap(map);
        }

        return branches;
    }
}
