document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('scanForm');
    const pathInput = document.getElementById('pathInput');
    const scanBtn = document.getElementById('scanBtn');
    const statusMsg = document.getElementById('statusMsg');
    const layoutBtn = document.getElementById('layoutBtn');
    const exportBtn = document.getElementById('exportBtn');

    let cy = null;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const path = pathInput.value.trim();
        if (!path) return;

        setLoading(true);
        statusMsg.style.display = 'none';

        try {
            const response = await fetch('/api/scan', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ path })
            });

            if (!response.ok) {
                throw new Error(await response.text());
            }

            const data = await response.json();
            renderGraph(data);

        } catch (error) {
            console.error(error);
            statusMsg.textContent = 'Error: ' + error.message;
            statusMsg.style.display = 'block';
            statusMsg.style.color = '#ef4444';
        } finally {
            setLoading(false);
        }
    });

    layoutBtn.addEventListener('click', () => {
        if (cy) {
            cy.layout(getLayoutConfig()).run();
        }
    });

    exportBtn.addEventListener('click', () => {
        if (cy) {
            const png = cy.png({ full: true, scale: 2 });
            const link = document.createElement('a');
            link.href = png;
            link.download = 'dependency-graph.png';
            link.click();
        }
    });

    function setLoading(isLoading) {
        if (isLoading) {
            scanBtn.classList.add('loading');
            scanBtn.disabled = true;
        } else {
            scanBtn.classList.remove('loading');
            scanBtn.disabled = false;
        }
    }

    function renderGraph(elements) {
        if (cy) {
            cy.destroy();
        }

        if (!elements.elements.nodes || elements.elements.nodes.length === 0) {
            statusMsg.textContent = 'No dependencies found.';
            statusMsg.style.display = 'block';
            statusMsg.style.color = '#f59e0b';
            return;
        }

        cy = cytoscape({
            container: document.getElementById('cy'),
            elements: elements.elements,
            style: [
                {
                    selector: 'node',
                    style: {
                        'background-color': '#3b82f6',
                        'label': 'data(label)',
                        'color': '#f8fafc',
                        'font-size': '12px',
                        'text-valign': 'bottom',
                        'text-margin-y': 5,
                        'width': 40,
                        'height': 40,
                        'text-outline-width': 2,
                        'text-outline-color': '#1e293b'
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 2,
                        'line-color': '#475569',
                        'target-arrow-color': '#475569',
                        'target-arrow-shape': 'triangle',
                        'curve-style': 'bezier',
                        'label': 'data(label)',
                        'font-size': '10px',
                        'color': '#94a3b8',
                        'text-rotation': 'autorotate',
                        'text-background-color': '#0f172a',
                        'text-background-opacity': 1,
                        'text-background-padding': 2,
                        'width': 2,
                        'line-color': '#475569',
                        'target-arrow-color': '#475569'
                    }
                },
                {
                    selector: ':selected',
                    style: {
                        'background-color': '#a78bfa',
                        'line-color': '#a78bfa',
                        'target-arrow-color': '#a78bfa',
                        'source-arrow-color': '#a78bfa'
                    }
                }
            ],
            layout: getLayoutConfig()
        });

        cy.on('tap', 'node', function(evt){
            const node = evt.target;
            console.log('Clicked ' + node.id());
        });
    }

    function getLayoutConfig() {
        return {
            name: 'cose',
            animate: true,
            randomize: false,
            componentSpacing: 100,
            nodeRepulsion: function( node ){ return 400000; },
            nodeOverlap: 10,
            idealEdgeLength: function( edge ){ return 100; },
            edgeElasticity: function( edge ){ return 100; },
            nestingFactor: 5,
            gravity: 80,
            numIter: 1000,
            initialTemp: 200,
            coolingFactor: 0.95,
            minTemp: 1.0
        };
    }
});
