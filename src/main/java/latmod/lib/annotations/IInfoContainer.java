package latmod.lib.annotations;

/**
 * Created by LatvianModder on 26.03.2016.
 */
public interface IInfoContainer extends IAnnotationContainer {

    void setInfo(String[] s);

    String[] getInfo();
}
