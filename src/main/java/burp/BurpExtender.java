package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public final class BurpExtender implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Proxy History Colorer");
        api.logging().logToOutput("Proxy History Colorer loaded");
    }
}
