package serverutils.data;

public interface IPauseWhenEmpty {

    int serverUtilities$getPauseWhenEmptySeconds();

    void serverUtilities$setPauseWhenEmptySeconds(int value, boolean oneshot);
}
