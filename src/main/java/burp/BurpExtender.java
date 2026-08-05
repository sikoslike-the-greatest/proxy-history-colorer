package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import dev.proxyhistorycolorer.proxy.EndpointAnnotationHandler;
import dev.proxyhistorycolorer.store.EndpointRegistry;
import dev.proxyhistorycolorer.store.ProjectStore;
import dev.proxyhistorycolorer.store.RegistryCodec;
import dev.proxyhistorycolorer.sync.OrganizerSynchronizer;

public final class BurpExtender implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Proxy History Colorer");

        EndpointRegistry registry = new EndpointRegistry();
        ProjectStore projectStore = new ProjectStore(
                api.persistence().extensionData(),
                new RegistryCodec()
        );
        loadPersistedRegistry(api, registry, projectStore);

        OrganizerSynchronizer synchronizer = new OrganizerSynchronizer(
                api.organizer(),
                registry,
                projectStore,
                api.logging()
        );
        api.proxy().registerRequestHandler(new EndpointAnnotationHandler(registry));
        api.extension().registerUnloadingHandler(synchronizer::close);
        synchronizer.start();

        api.logging().logToOutput(
                "Proxy History Colorer loaded with " + registry.snapshot().size() + " cached endpoints"
        );
    }

    private static void loadPersistedRegistry(
            MontoyaApi api,
            EndpointRegistry registry,
            ProjectStore projectStore
    ) {
        try {
            registry.replace(projectStore.load());
        } catch (RuntimeException exception) {
            api.logging().logToError(
                    "Could not load the persisted endpoint registry; Organizer will rebuild it: "
                            + exception.getMessage()
            );
        }
    }
}
