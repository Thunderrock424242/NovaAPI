package com.thunder.novaapi.mixin;

import net.neoforged.neoforge.client.model.data.ModelData;
import com.thunder.novaapi.RenderEngine.instancing.IModeledDataExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Injects VAO and Index Count methods into ModelData.
 */
@Mixin(ModelData.class)
public class ModelDataMixin implements IModeledDataExtensions {

    @Unique private int nova_vaoID;
    @Unique private int nova_indexCount;

    @Override
    public int getVAO() {
        return nova_vaoID;
    }

    @Override
    public void setVAO(int vaoID) {
        this.nova_vaoID = vaoID;
    }

    @Override
    public int getIndexCount() {
        return nova_indexCount;
    }

    @Override
    public void setIndexCount(int indexCount) {
        this.nova_indexCount = indexCount;
    }
}
