package latmod.lib.annotations;

/**
 * Created by LatvianModder on 26.03.2016.
 */
public interface INumberBoundsContainer extends IAnnotationContainer {

    void setBounds(double min, double max);

    double getMin();

    double getMax();
}
