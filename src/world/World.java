package world;

import camera.Camera;
import control.Controller;
import engine.Math3D;
import engine.Painter;
import engine.Timer;
import list.LList;
import particle.Particle;
import shapes.Shape;
import terrain.Terrain;
import world.interfaceelement.InterfaceElement;

public class World {
	public static final int CHUNK_SIZE = 10;
	public final int width, length, height;
	WorldChunk[][][] chunk;
	
	private LList<WorldElement> element;
	private LList<Particle> particle;
	private LList<InterfaceElement> backgroundElement;
	private LList<InterfaceElement> interfaceElement;
	
	public World(int chunkWidth, int chunkLength, int chunkHeight) {
		Timer.timeStart();
		width = chunkWidth * CHUNK_SIZE;
		length = chunkLength * CHUNK_SIZE;
		height = chunkHeight * CHUNK_SIZE;
		chunk = new WorldChunk[chunkWidth][chunkLength][chunkHeight];
		element = new LList<>();
		particle = new LList<>();
		backgroundElement = new LList<>();
		interfaceElement = new LList<>();
		Timer.timeEnd("world constructor");
		System.out.println("world chunk size: " + chunkWidth + " " + chunkLength + " " + chunkHeight);
	}
	
	public void addShape(int x, int y, int z, Shape shape) {
		int cx = x / CHUNK_SIZE;
		int cy = y / CHUNK_SIZE;
		int cz = z / CHUNK_SIZE;
		int sx = x - cx * CHUNK_SIZE;
		int sy = y - cy * CHUNK_SIZE;
		int sz = z - cz * CHUNK_SIZE;
		if (chunk[cx][cy][cz] == null)
			chunk[cx][cy][cz] = new WorldChunk(CHUNK_SIZE);
		chunk[cx][cy][cz].add(sx, sy, sz, shape);
	}
	
	public void addElement(WorldElement element) {
		this.element = this.element.add(element);
	}
	
	public void addBackgroundElement(InterfaceElement element) {
		backgroundElement = backgroundElement.add(element);
	}
	
	public void addInterfaceElement(InterfaceElement element) {
		interfaceElement = interfaceElement.add(element);
	}
	
	public void addParticle(Particle particle) {
		this.particle = this.particle.add(particle);
	}
	
	// DRAWING
	
	public void draw(Painter painter, Camera c) {
		drawBackground(painter);
		drawChunks(painter, c);
		drawInterface(painter);
	}
	
	private void drawBackground(Painter painter) {
		for (LList<InterfaceElement> e : backgroundElement)
			e.node.draw(painter);
	}
	
	private void drawInterface(Painter painter) {
		for (LList<InterfaceElement> e : interfaceElement)
			e.node.draw(painter);
	}
	
	private void drawChunks(Painter painter, Camera c) {
		int boundaries[] = c.cullBoundaries();
		int volumeRaw = (boundaries[1] - boundaries[0]) * (boundaries[3] - boundaries[2]) * (boundaries[5] - boundaries[4]) / 100000;
		
		boundaries[0] = Math3D.max(boundaries[0], 0);
		boundaries[1] = Math3D.min(boundaries[1], width - 1);
		boundaries[2] = Math3D.max(boundaries[2], 0);
		boundaries[3] = Math3D.min(boundaries[3], length - 1);
		boundaries[4] = Math3D.max(boundaries[4], 0);
		boundaries[5] = Math3D.min(boundaries[5], height - 1);
		int volumeBound = (boundaries[1] - boundaries[0]) * (boundaries[3] - boundaries[2]) * (boundaries[5] - boundaries[4]) / 100000;
		
		
		int[] fromChunkCoord = getChunkCoord(boundaries[0], boundaries[2], boundaries[4]);
		int[] toChunkCoord = getChunkCoord(boundaries[1], boundaries[3], boundaries[5]);
		int[] cameraChunkCoord = getChunkCoord((int) c.x, (int) c.y, (int) c.z);
		int volumeChunk = (fromChunkCoord[0] - toChunkCoord[0]) * (fromChunkCoord[1] - toChunkCoord[1]) * (fromChunkCoord[2] - toChunkCoord[2]) / -1;
		
		Painter.debugString[1] = "(unit 100,000) volume raw " + volumeRaw + " ; (unit 100,000) volume bound " + volumeBound + " ; volume chunk " + volumeChunk;
		
		for (int x = fromChunkCoord[0]; x < cameraChunkCoord[0]; x++)
			drawChunksRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, Math3D.RIGHT);
		for (int x = toChunkCoord[0]; x > cameraChunkCoord[0]; x--)
			drawChunksRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, Math3D.LEFT);
		drawChunksRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cameraChunkCoord[0], Math3D.NONE);
	}
	
	private void drawChunksRow(Painter painter, Camera c, int[] fromChunkCoord, int[] toChunkCoord, int[] cameraChunkCoord, int x, int xSide) {
		for (int y = fromChunkCoord[1]; y < cameraChunkCoord[1]; y++)
			drawChunksColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, y, xSide, Math3D.BACK);
		for (int y = toChunkCoord[1]; y > cameraChunkCoord[1]; y--)
			drawChunksColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, y, xSide, Math3D.FRONT);
		drawChunksColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, cameraChunkCoord[1], xSide, Math3D.NONE);
	}
	
	private void drawChunksColumn(Painter painter, Camera c, int[] fromChunkCoord, int[] toChunkCoord, int[] cameraChunkCoord, int x, int y, int xSide, int ySide) {
		for (int z = fromChunkCoord[2]; z < cameraChunkCoord[2]; z++)
			drawChunk(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, y, z, xSide, ySide, Math3D.TOP);
		for (int z = toChunkCoord[2]; z > cameraChunkCoord[2]; z--)
			drawChunk(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, y, z, xSide, ySide, Math3D.BOTTOM);
		drawChunk(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, x, y, cameraChunkCoord[2], xSide, ySide, Math3D.NONE);
	}
	
	private void drawChunk(Painter painter, Camera c, int[] fromChunkCoord, int[] toChunkCoord, int[] cameraChunkCoord, int cx, int cy, int cz, int xSide, int ySide, int zSide) {
		if (chunk[cx][cy][cz] == null || chunk[cx][cy][cz].isEmpty())
			return;
		if (xSide == Math3D.RIGHT) {
			int startx = cx == fromChunkCoord[0] ? fromChunkCoord[3] : 0;
			for (int x = startx; x < CHUNK_SIZE; x++)
				drawRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, ySide, zSide, x);
		} else if (xSide == Math3D.LEFT) {
			int endx = cx == toChunkCoord[0] ? toChunkCoord[3] : CHUNK_SIZE - 1;
			for (int x = endx; x >= 0; x--)
				drawRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, ySide, zSide, x);
		} else {
			int startx = cx == fromChunkCoord[0] ? fromChunkCoord[3] : 0;
			int endx = cx == toChunkCoord[0] ? toChunkCoord[3] : CHUNK_SIZE - 1;
			for (int x = startx; x < cameraChunkCoord[3]; x++)
				drawRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, Math3D.RIGHT, ySide, zSide, x);
			for (int x = endx; x > cameraChunkCoord[3]; x--)
				drawRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, Math3D.LEFT, ySide, zSide, x);
			drawRow(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, ySide, zSide, cameraChunkCoord[3]);
		}
	}
	
	private void drawRow(Painter painter, Camera c, int[] fromChunkCoord, int[] toChunkCoord, int[] cameraChunkCoord, int cx, int cy, int cz, int xSide, int ySide, int zSide, int x) {
		if (ySide == Math3D.BACK) {
			int starty = cy == fromChunkCoord[1] ? fromChunkCoord[4] : 0;
			for (int y = starty; y < CHUNK_SIZE; y++)
				drawColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, ySide, zSide, x, y);
		} else if (ySide == Math3D.FRONT) {
			int endy = cy == toChunkCoord[1] ? toChunkCoord[4] : CHUNK_SIZE - 1;
			for (int y = endy; y >= 0; y--)
				drawColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, ySide, zSide, x, y);
		} else {
			int starty = cy == fromChunkCoord[1] ? fromChunkCoord[4] : 0;
			int endy = cy == toChunkCoord[1] ? toChunkCoord[4] : CHUNK_SIZE - 1;
			for (int y = starty; y < cameraChunkCoord[4]; y++)
				drawColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, Math3D.BACK, zSide, x, y);
			for (int y = endy; y > cameraChunkCoord[4]; y--)
				drawColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, Math3D.FRONT, zSide, x, y);
			drawColumn(painter, c, fromChunkCoord, toChunkCoord, cameraChunkCoord, cx, cy, cz, xSide, ySide, zSide, x, cameraChunkCoord[4]);
		}
	}
	
	private void drawColumn(Painter painter, Camera c, int[] fromChunkCoord, int[] toChunkCoord, int[] cameraChunkCoord, int cx, int cy, int cz, int xSide, int ySide, int zSide, int x, int y) {
		if (zSide == Math3D.TOP) {
			int startz = cz == fromChunkCoord[2] ? fromChunkCoord[5] : 0;
			for (int z = startz; z < CHUNK_SIZE; z++)
				drawCell(painter, c, cx, cy, cz, xSide, ySide, zSide, x, y, z);
		} else if (zSide == Math3D.BOTTOM) {
			int endz = cz == toChunkCoord[2] ? toChunkCoord[5] : CHUNK_SIZE - 1;
			for (int z = endz; z >= 0; z--)
				drawCell(painter, c, cx, cy, cz, xSide, ySide, zSide, x, y, z);
		} else {
			int startz = cz == fromChunkCoord[2] ? fromChunkCoord[5] : 0;
			int endz = cz == toChunkCoord[2] ? toChunkCoord[5] : CHUNK_SIZE - 1;
			for (int z = startz; z < cameraChunkCoord[5]; z++)
				drawCell(painter, c, cx, cy, cz, xSide, ySide, Math3D.TOP, x, y, z);
			for (int z = endz; z > cameraChunkCoord[5]; z--)
				drawCell(painter, c, cx, cy, cz, xSide, ySide, Math3D.BOTTOM, x, y, z);
			drawCell(painter, c, cx, cy, cz, xSide, ySide, zSide, x, y, cameraChunkCoord[5]);
		}
	}
	
	private void drawCell(Painter painter, Camera c, int cx, int cy, int cz, int xSide, int ySide, int zSide, int x, int y, int z) {
		chunk[cx][cy][cz].draw(x, y, z, painter, c, xSide, ySide, zSide);
	}
	
	// UPDATE
	
	public void initWorldElements() {
		for (LList<WorldElement> e : element)
			e.node.init(this);
	}
	
	public void update(Terrain terrain, Controller controller) {
		for (LList<WorldElement> e : element)
			e.node.update(this, terrain, controller);
		for (LList<Particle> p : particle)
			if (p.node.update(this))
				particle = particle.remove(p);
	}
	
	// UITL
	
	private int[] getChunkCoord(int x, int y, int z) {
		int cx = x / CHUNK_SIZE;
		int cy = y / CHUNK_SIZE;
		int cz = z / CHUNK_SIZE;
		x -= cx * CHUNK_SIZE;
		y -= cy * CHUNK_SIZE;
		z -= cz * CHUNK_SIZE;
		return new int[] {cx, cy, cz, x, y, z};
	}
}
