package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.datatransfer.pdf.AssistantPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.CommerzbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaExtractor;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.SBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.UnicreditPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ImportExtractedItemsWizard extends Wizard
{
    private Client client;
    private List<Extractor> extractors = new ArrayList<>();
    private IPreferenceStore preferences;
    private List<File> files;

    private List<ReviewExtractedItemsPage> pages = new ArrayList<>();

    public ImportExtractedItemsWizard(Client client, Extractor extractor, IPreferenceStore preferences,
                    List<File> files) throws IOException
    {
        this.client = client;
        this.preferences = preferences;
        this.files = files;

        if (extractor != null)
            extractors.add(extractor);
        else
            addDefaultExtractors();

        setWindowTitle(Messages.PDFImportWizardTitle);
        setNeedsProgressMonitor(true);
    }

    private void addDefaultExtractors() throws IOException
    {
        extractors.add(new BaaderBankPDFExtractor(client));
        extractors.add(new BankSLMPDFExctractor(client));
        extractors.add(new ComdirectPDFExtractor(client));
        extractors.add(new CommerzbankPDFExctractor(client));
        extractors.add(new ConsorsbankPDFExctractor(client));
        extractors.add(new DABPDFExctractor(client));
        extractors.add(new DegiroPDFExtractor(client));
        extractors.add(new DeutscheBankPDFExctractor(client));
        extractors.add(new DkbPDFExtractor(client));
        extractors.add(new FinTechGroupBankPDFExtractor(client));
        extractors.add(new INGDiBaExtractor(client));
        extractors.add(new OnvistaPDFExtractor(client));
        extractors.add(new SBrokerPDFExtractor(client));
        extractors.add(new UnicreditPDFExtractor(client));
    }

    @Override
    public boolean canFinish()
    {
        // allow "Finish" only on the last page
        return getContainer().getCurrentPage() == pages.get(pages.size() - 1);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        // assign files to extractors and create a page for each extractor that
        // has a file

        Map<Extractor, List<File>> extractor2files = new HashMap<>();

        if (extractors.size() == 1)
            extractor2files.put(extractors.get(0), files);
        else
            assignFilesToExtractors(extractor2files);

        for (Extractor extractor : extractors)
        {
            List<File> files4extractor = extractor2files.get(extractor);
            if (files4extractor == null || files4extractor.isEmpty())
                continue;

            ReviewExtractedItemsPage page = new ReviewExtractedItemsPage(client, extractor, preferences,
                            files4extractor);
            pages.add(page);
            addPage(page);
        }
        
        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    private void assignFilesToExtractors(Map<Extractor, List<File>> extractor2files)
    {
        try
        {
            List<File> unknown = new ArrayList<>();

            for (File file : files)
            {
                Extractor extractor = PDFImportAssistant.detectBankIdentifier(file, extractors);

                if (extractor != null)
                    extractor2files.computeIfAbsent(extractor, k -> new ArrayList<>()).add(file);
                else
                    unknown.add(file);
            }

            if (!unknown.isEmpty())
            {
                Extractor e = new AssistantPDFExtractor(client, new ArrayList<>(extractors));
                extractors.add(e);
                extractor2files.put(e, unknown);
            }
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean performFinish()
    {

        if (pages.size() > 0)
        {
            boolean isDirty = false;
            for (int PDFAssistantPageID = 0; PDFAssistantPageID < pages.size(); PDFAssistantPageID++)
            {

                pages.get(PDFAssistantPageID).afterPage();
                InsertAction action = new InsertAction(client);
                action.setConvertBuySellToDelivery(pages.get(PDFAssistantPageID).doConvertToDelivery());

                for (ExtractedEntry entry : pages.get(PDFAssistantPageID).getEntries())
                {
                    if (entry.isImported())
                    {
                        entry.getItem().apply(action, pages.get(PDFAssistantPageID));
                        isDirty = true;
                    }
                }
            }

            if (isDirty)
            {
                client.markDirty();

                // run consistency checks in case bogus transactions have been
                // created (say: an outbound delivery of a security where there
                // no held shares)
                new ConsistencyChecksJob(client, false).schedule();
            }
        }

        return true;
    }
}
