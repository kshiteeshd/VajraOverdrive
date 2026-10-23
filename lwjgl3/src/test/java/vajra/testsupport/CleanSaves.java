package vajra.testsupport;

import java.io.File;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Every test class starts with no saved pilot or run in the test data folder (build/testdata, see the
 *  module's vajra.data property): a run another test left parked must never turn the title's first row
 *  into CONTINUE for the next one. Auto-registered through META-INF/services. */
public final class CleanSaves implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        for (String f : new String[] {"profiles.json", "vajra.save"})
            new File("build/testdata/" + f).delete();
    }
}
