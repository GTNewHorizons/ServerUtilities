package latmod.lib.github;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import latmod.lib.net.LMURLConnection;
import latmod.lib.net.RequestMethod;
import latmod.lib.util.FinalIDObject;

/**
 * Created by LatvianModder on 16.03.2016.
 */
public class GitHubCommit extends FinalIDObject {

    public final GitHubBranch branch;
    public final String info;
    public final String treeURL;
    private Map<String, GitHubFile.Blob> files;

    public GitHubCommit(GitHubBranch b, JsonObject o) {
        super(o.get("sha").getAsString());
        o = o.get("commit").getAsJsonObject();
        info = o.get("message").getAsString();
        treeURL = o.get("tree").getAsJsonObject().get("url").getAsString();
        // tree = new GuideTree(o.get("tree").getAsJsonObject());
        // System.out.println("Tree: " + getTree());
        branch = b;
    }

    public String toString() {
        return info.isEmpty() ? getID() : info;
    }

    public Map<String, GitHubFile.Blob> getFiles() throws Exception {
        if (files == null) {
            Map<String, GitHubFile.Blob> map = new LinkedHashMap<>();

            for (JsonElement e : new LMURLConnection(RequestMethod.SIMPLE_GET, treeURL).connect().asJson()
                    .getAsJsonObject().get("tree").getAsJsonArray()) {
                addToTree(map, GitHubFile.getFrom(this, null, e.getAsJsonObject()));
            }

            files = Collections.unmodifiableMap(map);
        }

        return files;
    }

    private void addToTree(Map<String, GitHubFile.Blob> map, GitHubFile file) throws Exception {
        if (file instanceof GitHubFile.Tree) {
            JsonObject o = new LMURLConnection(RequestMethod.SIMPLE_GET, file.url).connect().asJson().getAsJsonObject();

            for (JsonElement e : o.get("tree").getAsJsonArray()) {
                addToTree(map, GitHubFile.getFrom(this, file, e.getAsJsonObject()));
            }
        } else {
            map.put(file.getID(), (GitHubFile.Blob) file);
        }
    }

    public String getFilePath(String s) {
        return GitHubAPI.RAW_CONTENT + branch.repo.getID() + "/" + getID() + "/" + s;
    }
}
