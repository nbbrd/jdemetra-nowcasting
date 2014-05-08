/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package be.nbb.demetra.dfm;

import ec.nbdemetra.ws.DefaultFileItemRepository;
import ec.nbdemetra.ws.IWorkspaceItemRepository;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.MetaData;
import java.util.Date;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author palatej
 */
@ServiceProvider(service = IWorkspaceItemRepository.class)
public class DfmDocFileRepository extends DefaultFileItemRepository<VersionedDfmDocument> {

    public static final String REPOSITORY = "DfmDoc";

    @Override
    public String getRepository() {
        return REPOSITORY;
    }

    @Override
    public Class<VersionedDfmDocument> getSupportedType() {
        return VersionedDfmDocument.class;
    }

    @Override
    public boolean save(WorkspaceItem<VersionedDfmDocument> doc) {
        VersionedDfmDocument element = doc.getElement();
        element.getCurrent().getMetaData().put(MetaData.DATE, new Date().toString());
        return super.save(doc);
    }}
