package latmod.lib.annotations;

/**
 * Created by LatvianModder on 26.03.2016.
 */
public interface IFlagContainer extends IAnnotationContainer {

    void setFlag(byte flag, boolean b);

    boolean getFlag(byte flag);
}
