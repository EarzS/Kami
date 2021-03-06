package terrain;

import engine.Math3D;
import engine.Timer;
import list.LList;
import terrain.terrainmodule.FullGray;
import terrain.terrainmodule.TerrainModule;
import world.World;

public class Terrain {
	private static final int CHUNK_SIZE = 20, CHUNK_BUFFER = 2;
	public int width, length, height;
	private TerrainChunk[][][] terrainChunk;
	private IntersectionFinder intersectionFinder;
	private LList<TerrainChunk> dirtyDrawChunk;
	
	public Terrain() {
		Timer.timeStart();
		intersectionFinder = new IntersectionFinder(this);
		terrainChunk = new TerrainChunk[500][500][10];
		width = terrainChunk.length * CHUNK_SIZE;
		length = terrainChunk[0].length * CHUNK_SIZE;
		height = terrainChunk[0][0].length * CHUNK_SIZE;
		Timer.timeEnd("terrain constructor");
		System.out.println("terrain size " + width + " " + length + " " + height);
	}
	
	private void generate(int cx, int cy, int cz) {
		dirtyDrawChunk = new LList();
		dirtyDrawChunk = dirtyDrawChunk.add(terrainChunk[cx][cy][cz]);
		terrainChunk[cx][cy][cz].generated = true;
		if (cz == 0) {
			for (int x = 0; x < CHUNK_SIZE; x++)
				for (int y = 0; y < CHUNK_SIZE; y++) {
					terrainChunk[cx][cy][cz].add(x, y, 0, new FullGray());
					if (Math.random() > .9993)
						generateTree(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y, 0);
				}
			if (cx == 3 && cy == 0) {
				for (int x = 0; x < 100; x++)
					for (int z = 3; z < 10; z++)
						add(x, 5, z, new FullGray());
			}
		}
	}
	
	private void add(int x, int y, int z, TerrainModule terrainModule) {
		int[] coord = getChunkCoord(x, y, z);
		if (terrainChunk[coord[0]][coord[1]][coord[2]] == null)
			terrainChunk[coord[0]][coord[1]][coord[2]] = new TerrainChunk(coord[0] * CHUNK_SIZE, coord[1] * CHUNK_SIZE, coord[2] * CHUNK_SIZE, CHUNK_SIZE);
		if (!terrainChunk[coord[0]][coord[1]][coord[2]].drawDirty)
			dirtyDrawChunk = dirtyDrawChunk.add(terrainChunk[coord[0]][coord[1]][coord[2]]);
		terrainChunk[coord[0]][coord[1]][coord[2]].add(coord[3], coord[4], coord[5], terrainModule);
	}
	
	private void generateTree(int x, int y, int floor) {
		int thick = 5;
		x = Math3D.maxMin(x, width - thick * 2, thick);
		y = Math3D.maxMin(y, length - thick * 2, thick);
		int height = Math3D.rand(20, 30) + 10;
		int brushSpread = Math3D.rand(1, 3) + height / 5 - 4;
		int brushHeight = Math3D.rand(1, 3);
		for (int xi = x - thick; xi <= x + thick; xi++)
			for (int yi = y - thick; yi <= y + thick; yi++)
				for (int z = floor; z < height + floor; z++)
					add(xi, yi, z, new FullGray());
		int xs = Math3D.max(x - brushSpread, 0);
		int xe = Math3D.min(x + brushSpread, width - 1);
		int ys = Math3D.max(y - brushSpread, 0);
		int ye = Math3D.min(y + brushSpread, length - 1);
		for (int xi = xs; xi <= xe; xi++)
			for (int yi = ys; yi <= ye; yi++)
				for (int z = height + floor; z < height + brushHeight + floor; z++)
					add(xi, yi, z, new FullGray());
	}
	
	public void expand(int x, int y, int z, World world) {
		int[] coord = getChunkCoord(x, y, z);
		for (int xi = -CHUNK_BUFFER; xi <= CHUNK_BUFFER; xi++)
			for (int yi = -CHUNK_BUFFER; yi <= CHUNK_BUFFER; yi++)
				for (int zi = -CHUNK_BUFFER; zi <= CHUNK_BUFFER; zi++)
					expandChunk(coord[0] + xi, coord[1] + yi, coord[2] + zi, world);
	}
	
	private void expandChunk(int cx, int cy, int cz, World world) {
		if (cx < 0 || cx >= terrainChunk.length || cy < 0 || cy >= terrainChunk[0].length || cz < 0 || cz >= terrainChunk[0][0].length)
			return;
		if (terrainChunk[cx][cy][cz] == null)
			terrainChunk[cx][cy][cz] = new TerrainChunk(cx * CHUNK_SIZE, cy * CHUNK_SIZE, cz * CHUNK_SIZE, CHUNK_SIZE);
		if (!terrainChunk[cx][cy][cz].generated) {
			generate(cx, cy, cz);
			addToWorld(world);
		}
	}
	
	private void addToWorld(World world) {
		for (LList<TerrainChunk> c : dirtyDrawChunk)
			c.node.addToWorld(world);
	}
	
	public double[] findIntersection(double[] orig, double[] dir, boolean allowSlide, boolean limitDistance, boolean allowCollideWithEdge) {
		return intersectionFinder.find(orig, dir, allowSlide, limitDistance, allowCollideWithEdge);
	}
	
	public boolean[] getIntersectionCollide() {
		return intersectionFinder.collide;
	}
	
	private int[] getChunkCoord(int x, int y, int z) {
		int cx = x / CHUNK_SIZE;
		int cy = y / CHUNK_SIZE;
		int cz = z / CHUNK_SIZE;
		x -= cx * CHUNK_SIZE;
		y -= cy * CHUNK_SIZE;
		z -= cz * CHUNK_SIZE;
		return new int[] {cx, cy, cz, x, y, z};
	}
	
	boolean isEmpty(int x, int y, int z) {
		int[] coord = getChunkCoord(x, y, z);
		return terrainChunk[coord[0]][coord[1]][coord[2]] != null && terrainChunk[coord[0]][coord[1]][coord[2]].isEmpty(coord[3], coord[4], coord[5]);
	}
	
	boolean isInBounds(int x, int y, int z) {
		if (x >= 1 && y >= 1 && z >= 1 && x < width - 2 && y < length - 2 && z < height - 2) {
			int[] coord = getChunkCoord(x, y, z);
			if (terrainChunk[coord[0]][coord[1]][coord[2]] != null)
				return true;
		}
		return false;
	}
}
