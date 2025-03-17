package com.thunder.NovaAPI.RenderEngine.instancing;

/**
 * Interface that exposes injected ModelData methods.
 */
public interface IModeledDataExtensions {
    int getVAO();
    void setVAO(int vaoID);
    int getIndexCount();
    void setIndexCount(int indexCount);
}