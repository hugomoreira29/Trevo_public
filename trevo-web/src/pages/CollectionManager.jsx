import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Pencil, Trash2, Plus, GripVertical, X } from "lucide-react";
import {
  createCollection,
  deleteCollection,
  getShopProfile,
  renameCollection,
  reorderCollections,
} from "../services/shopService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

function sortCollections(collections) {
  return [...collections].sort((a, b) => a.displayOrder - b.displayOrder);
}

function moveCollection(collections, draggedId, targetId) {
  if (!draggedId || draggedId === targetId) return collections;

  const nextCollections = [...collections];
  const fromIndex = nextCollections.findIndex((c) => c.id === draggedId);
  const toIndex = nextCollections.findIndex((c) => c.id === targetId);

  if (fromIndex < 0 || toIndex < 0) return collections;

  const [dragged] = nextCollections.splice(fromIndex, 1);
  nextCollections.splice(toIndex, 0, dragged);

  return nextCollections.map((collection, index) => ({
    ...collection,
    displayOrder: index,
  }));
}

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

export default function CollectionManager() {
  const { shopId } = useParams();
  const [shopName, setShopName] = useState("");
  const [collections, setCollections] = useState([]);
  const [newCollectionName, setNewCollectionName] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editingName, setEditingName] = useState("");
  const [collectionToDelete, setCollectionToDelete] = useState(null);
  const [draggedId, setDraggedId] = useState(null);
  const [dropTargetId, setDropTargetId] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadCollections() {
      try {
        const profile = await getShopProfile(shopId);
        if (active) {
          setShopName(profile.name || "");
          setCollections(sortCollections(profile.collections || []));
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, "Não foi possível carregar as coleções."));
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadCollections();
    return () => {
      active = false;
    };
  }, [shopId]);

  async function handleCreate(event) {
    event.preventDefault();
    setError("");

    const name = newCollectionName.trim();
    if (!name) {
      setError("Preenche o nome da coleção.");
      return;
    }

    try {
      setIsSubmitting(true);
      const created = await createCollection(shopId, name);
      setCollections((current) => sortCollections([...current, created]));
      setNewCollectionName("");
    } catch (createError) {
      setError(getErrorMessage(createError, "Não foi possível criar a coleção."));
    } finally {
      setIsSubmitting(false);
    }
  }

  function startRename(collection) {
    setEditingId(collection.id);
    setEditingName(collection.name);
  }

  async function handleRename(event) {
    event.preventDefault();
    setError("");

    const name = editingName.trim();
    if (!name) {
      setError("Preenche o nome da coleção.");
      return;
    }

    try {
      setIsSubmitting(true);
      const updated = await renameCollection(shopId, editingId, name);
      setCollections((current) =>
        sortCollections(current.map((c) => (c.id === updated.id ? updated : c)))
      );
      setEditingId(null);
      setEditingName("");
    } catch (renameError) {
      setError(getErrorMessage(renameError, "Não foi possível renomear a coleção."));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function confirmDelete() {
    if (!collectionToDelete) return;

    try {
      setIsSubmitting(true);
      await deleteCollection(shopId, collectionToDelete.id);
      setCollections((current) => current.filter((c) => c.id !== collectionToDelete.id));
      setCollectionToDelete(null);
      setError("");
    } catch (deleteError) {
      setError(getErrorMessage(deleteError, "Não foi possível apagar a coleção."));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDrop(targetId) {
    const nextCollections = moveCollection(collections, draggedId, targetId);
    setDraggedId(null);
    setDropTargetId(null);

    if (nextCollections === collections) return;

    setCollections(nextCollections);

    try {
      const reordered = await reorderCollections(
        shopId,
        nextCollections.map((c) => c.id)
      );
      setCollections(sortCollections(reordered));
    } catch (reorderError) {
      setError(getErrorMessage(reorderError, "Não foi possível reordenar as coleções."));
    }
  }

  return (
    <div className="page-shell-narrow">
      <p className="eyebrow text-clover-700">Loja · Catálogo</p>
      <h1 className="h-page mt-2">Gerir coleções</h1>
      {shopName && <p className="mt-2 text-ink-500">Loja: <span className="text-ink-900 font-medium">{shopName}</span></p>}

      {isLoading && <LoadingState label="A carregar coleções..." />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}

      {!isLoading && (
        <>
          <form onSubmit={handleCreate} className="mt-6 flex gap-2">
            <input
              type="text"
              name="collectionName"
              placeholder="Nome da coleção"
              value={newCollectionName}
              onChange={(event) => setNewCollectionName(event.target.value)}
              className="input flex-1"
            />
            <button type="submit" disabled={isSubmitting} className="btn-primary shrink-0">
              <Plus className="w-4 h-4" strokeWidth={1.8} />
              Criar
            </button>
          </form>

          {collections.length === 0 && (
            <p className="mt-6 text-sm text-ink-500">Esta loja ainda não tem coleções.</p>
          )}

          <ul className="mt-6 flex flex-col gap-2">
            {collections.map((collection) => (
              <li
                key={collection.id}
                draggable
                onDragStart={() => setDraggedId(collection.id)}
                onDragEnd={() => {
                  setDraggedId(null);
                  setDropTargetId(null);
                }}
                onDragEnter={() => setDropTargetId(collection.id)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => handleDrop(collection.id)}
                className={
                  "rounded border p-3 transition " +
                  (draggedId === collection.id
                    ? "border-ink-300 bg-ink-50 opacity-60"
                    : dropTargetId === collection.id
                    ? "border-clover-500 bg-clover-50/40"
                    : "border-ink-200 bg-white hover:border-ink-300")
                }
              >
                {editingId === collection.id ? (
                  <form onSubmit={handleRename} className="flex gap-2">
                    <input
                      type="text"
                      name="renameCollection"
                      value={editingName}
                      onChange={(event) => setEditingName(event.target.value)}
                      className="input flex-1"
                      autoFocus
                    />
                    <button type="submit" disabled={isSubmitting} className="btn-primary shrink-0">
                      Guardar
                    </button>
                    <button
                      type="button"
                      onClick={() => { setEditingId(null); setEditingName(""); }}
                      className="btn-secondary shrink-0"
                    >
                      Cancelar
                    </button>
                  </form>
                ) : (
                  <div className="flex items-center gap-3">
                    <GripVertical className="w-4 h-4 text-ink-300 cursor-grab" strokeWidth={1.7} />
                    <span className="font-medium">{collection.name}</span>
                    <button
                      type="button"
                      onClick={() => startRename(collection)}
                      aria-label="Renomear"
                      className="ml-auto w-8 h-8 inline-flex items-center justify-center rounded border border-ink-200 text-ink-600 hover:bg-ink-50 transition-colors"
                    >
                      <Pencil className="w-3.5 h-3.5" strokeWidth={1.7} />
                    </button>
                    <button
                      type="button"
                      onClick={() => setCollectionToDelete(collection)}
                      aria-label="Apagar"
                      className="w-8 h-8 inline-flex items-center justify-center rounded border border-ink-200 text-ink-600 hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition-colors"
                    >
                      <Trash2 className="w-3.5 h-3.5" strokeWidth={1.7} />
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </>
      )}

      {collectionToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/45 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-collection-title"
            className="w-full max-w-md rounded-md bg-white p-6 shadow-xl"
          >
            <div className="flex items-start justify-between gap-4">
              <h2 id="delete-collection-title" className="text-lg font-semibold tracking-tight">
                Apagar coleção
              </h2>
              <button
                type="button"
                onClick={() => setCollectionToDelete(null)}
                className="w-8 h-8 rounded inline-flex items-center justify-center border border-ink-200 hover:bg-ink-50 transition-colors"
                aria-label="Fechar"
              >
                <X className="w-4 h-4" strokeWidth={1.8} />
              </button>
            </div>
            <p className="mt-3 text-sm text-ink-600">
              Vais apagar a coleção <strong className="text-ink-900">{collectionToDelete.name}</strong>. Os produtos manterão-se mas deixarão de estar agrupados.
            </p>
            <div className="mt-6 flex justify-end gap-2">
              <button type="button" onClick={() => setCollectionToDelete(null)} className="btn-secondary">
                Cancelar
              </button>
              <button
                type="button"
                onClick={confirmDelete}
                disabled={isSubmitting}
                className="btn-danger"
              >
                <Trash2 className="w-4 h-4" strokeWidth={1.7} />
                {isSubmitting ? "A apagar..." : "Apagar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
