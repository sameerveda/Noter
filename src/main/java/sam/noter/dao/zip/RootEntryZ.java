package sam.noter.dao.zip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import sam.myutils.MyUtilsCheck;
import sam.noter.dao.Entry;
import sam.noter.dao.ModifiedField;
import sam.noter.dao.RootEntry;
import sam.noter.dao.zip.RootEntryZFactory.CacheDir;

class RootEntryZ extends EntryZ implements RootEntry {
	private CacheDir cacheDir;
	private Runnable onModified;
	private HashSet<Entry> entries;
	private List<Entry> removed;

	public RootEntryZ(CacheDir cacheDir) throws Exception {
		super(null, RootEntry.ROOT_ENTRY_ID, "ROOT", false);
		this.cacheDir = cacheDir;
		reload();
	}

	@Override public void close() throws Exception {/* DOES  NOTHING */ }

	@Override
	public File getJbookPath() {
		return cacheDir == null ? null : cacheDir.getSourceFile() == null ? null : cacheDir.getSourceFile().toFile();
	}
	@Override
	public void setJbookPath(File path) {
		Objects.requireNonNull(path);
		cacheDir.setSourceFile(path.toPath());
	}

	@Override
	public boolean isModified() {
		return childrenM;
	}
	void setModified() {
		childrenM = true;
	}

	@Override
	public void reload() throws Exception {
		setItems(cacheDir.loadEntries(this));
	}
	@Override
	public void setItems(List<EntryZ> items) {
		this.items.setAll(items);
		entries = new HashSet<>(entries == null ? 50 : entries.size()+10);
		walk(entries::add);
		childrenM = false;
		onModified();
	}
	@Override
	public void save(File file) throws Exception {
		cacheDir.save(this, file);
		entries.forEach(e -> cast(e).clearModified());
		childrenM = false;
		onModified();
	}
	private EntryZ cast(Entry e) {
		return (EntryZ)e;
	}

	@Override
	public void setOnModified(Runnable onModified) {
		this.onModified = onModified;
	}
	private void onModified() {
		if(onModified != null) onModified.run();
	}
	@Override
	protected void childModified(ModifiedField field, Entry child, Entry modifiedEntry) {
		childrenM = true;
		onModified();
	}
	@Override protected void notifyParent(ModifiedField field) { }

	@Override
	public Collection<Entry> getAllEntries() {
		return Collections.unmodifiableSet(entries);
	}

	@Override
	public Entry addChild(String title, Entry parent, int index) {
		EntryZ e = cacheDir.newEntry(title, this);
		addChild(e, parent, index);
		return e;
	}

	@Override
	public List<Entry> moveChild(List<Entry> childrenToMove, Entry newParent, int index) {
		if(MyUtilsCheck.isEmpty(childrenToMove)) return Collections.emptyList();

		EntryZ parent = castCheckRoot(newParent);

		if(childrenToMove.stream().allMatch(c -> castNonNull(c).getRoot() == this)) {
			childrenToMove.stream()
			.peek(e -> checkIfSame(parent, e))
			.collect(Collectors.groupingBy(Entry::parent))
			.forEach((p, children) -> cast(p).modifiableChildren(l -> l.removeAll(children)));
			
			parent.addAll(childrenToMove, index);
			return childrenToMove;
		}
		
		childrenToMove.stream()
		.peek(e -> checkIfSame(parent, e))
		.collect(Collectors.groupingBy(e -> castNonNull(e).getRoot()))
		.forEach((root, children) -> root.entries.removeAll(children));
		
		childrenToMove.stream()
		.peek(e -> checkIfSame(parent, e))
		.collect(Collectors.groupingBy(Entry::parent))
		.forEach((p, childrn) -> castNonNull(p).modifiableChildren(list -> list.removeAll(childrn)));

		List<Entry> list = childrenToMove.stream()
				.map(c -> changeRoot(castNonNull(c)))
				.peek(entries::add)
				.collect(Collectors.toList());

		childrenToMove = null;
		cast(newParent).addAll(list, index);
		return list;
	}
	
	private EntryZ changeRoot(EntryZ d) {
		RootEntryZ root = cast(d).getRoot(); 
		if(root == this) return d;

		EntryZ result = cacheDir.newEntry(d, this);
		root.entries.remove(d);

		if(d.getChildren().isEmpty()) return result;
		result.modifiableChildren(list -> list.addAll(d.getChildren().stream().map(t -> changeRoot(castNonNull(t))).collect(Collectors.toList())));
		return result;
	}
	
	private void checkIfSame(Entry parent, Entry child) {
		if(parent == child)
			throw new IllegalArgumentException("child and parent are same Entry");
	}
	
	@Override
	public void addChild(Entry child, Entry parent, int index) {
		EntryZ c = castCheckRoot(child);
		EntryZ p = castCheckRoot(parent);
		checkIfSame(p, c);
		
		
		entries.add(c);
		if(!MyUtilsCheck.isEmpty(removed))
			removed.remove(child);
		
		p.add(c, index);
	}
	private EntryZ castNonNull(Object object) {
		return Objects.requireNonNull(cast(object));
	}
	private EntryZ cast(Object object) {
		return (EntryZ) object;
	}
	
	private EntryZ castCheckRoot(Object e) {
		EntryZ d = castNonNull(e);
		if(d != this && d.getRoot() != this)
			throw new IllegalStateException(e+"  is not part of current root");

		return d;
	}

	@Override
	public void removeFromParent(Entry child) {
		EntryZ d = castCheckRoot(child);
		if(removed == null)
			removed = new ArrayList<>();
		entries.remove(d);
		removed.add(d);
		castNonNull(d.parent()).modifiableChildren(l -> l.remove(d));
	}

	 String getContent(EntryZ e) throws IOException {
		return cacheDir.getContent(e);
	}

	public CacheDir getCacheDir() {
		return cacheDir;
	}
	
}
