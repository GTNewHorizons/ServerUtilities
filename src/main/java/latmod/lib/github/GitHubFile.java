package latmod.lib.github;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import latmod.lib.util.FinalIDObject;

/**
 * Created by LatvianModder on 16.03.2016.
 */
public abstract class GitHubFile extends FinalIDObject {

    public final GitHubCommit commit;
    public final GitHubFile parent;

    public final String url;
    public final String path;

    public GitHubFile(GitHubCommit p, GitHubFile pt, JsonObject o) throws Exception {
        super(o.get("sha").getAsString());
        commit = p;
        parent = pt;

        url = o.get("url").getAsString();
        path = o.get("path").getAsString();
    }

    public String getPath() {
        return parent == null ? path : (parent.getPath() + "/" + path);
    }

    public String toString() {
        return getPath();
    }

    public static GitHubFile getFrom(GitHubCommit commit, GitHubFile parent, JsonObject o) throws Exception {
        if (o.get("type").getAsString().equals("tree")) {
            return new Tree(commit, parent, o);
        } else {
            return new Blob(commit, parent, o);
        }
    }

    public static class Blob extends GitHubFile {

        public final String pathURL;

        public final int size;
        public final String encoding;

        public Blob(GitHubCommit p, GitHubFile pt, JsonObject o) throws Exception {
            super(p, pt, o);

            pathURL = commit.getFilePath(getPath()).replace(" ", "%20");

            size = o.get("size").getAsInt();
            encoding = o.has("encoding") ? o.get("encoding").getAsString() : "";
        }
    }

    public static class Tree extends GitHubFile {

        public final List<JsonObject> tree;

        public Tree(GitHubCommit p, GitHubFile pt, JsonObject o) throws Exception {
            super(p, pt, o);

            List<JsonObject> l = new ArrayList<>();

            if (o.has("tree")) {
                JsonArray a = o.get("tree").getAsJsonArray();

                for (JsonElement e : a) {
                    // l.add(new GitHubTree(this, e.getAsJsonObject()));
                }
            }

            tree = Collections.unmodifiableList(l);
        }
    }
}
