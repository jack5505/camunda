import {expect} from 'chai';
import {setEndEvent, unsetEndEvent, setGateway, unsetGateway, leaveGatewayAnalysisMode,
        hoverElement, addBranchOverlay, BRANCH_OVERLAY, showSelectedOverlay, isValidElement,
        __set__, __ResetDependency__} from 'main/processDisplay/diagram/analytics/service';
import sinon from 'sinon';

describe('Analytics service', () => {
  const ENTER_GATEWAY_ANALYSIS_MODE = 'ENTER_GATEWAY_ANALYSIS_MODE';
  const SET_ELEMENT = 'SET_ELEMENT';

  let heatmapData;
  let dispatchAction;
  let createEnterGatewayAnalysisModeAction;
  let createSetElementAction;
  let updateOverlayVisibility;
  let isBpmnType;
  let overlayNode;

  let viewer;

  beforeEach(() => {
    heatmapData = {
      piCount: 10,
      flowNodes: {
        a1: 5
      }
    };

    dispatchAction = sinon.spy();
    __set__('dispatchAction', dispatchAction);

    updateOverlayVisibility = sinon.spy();
    __set__('updateOverlayVisibility', updateOverlayVisibility);

    createEnterGatewayAnalysisModeAction = sinon.stub().returns(ENTER_GATEWAY_ANALYSIS_MODE);
    __set__('createEnterGatewayAnalysisModeAction', createEnterGatewayAnalysisModeAction);

    isBpmnType = sinon.stub().returns(true);
    __set__('isBpmnType', isBpmnType);

    createSetElementAction = sinon.stub().returns(SET_ELEMENT);
    __set__('createSetElementAction', createSetElementAction);

    overlayNode = {
      html: document.createElement('div')
    };
    viewer = {
      get: sinon.stub().returnsThis(),
      add: sinon.spy(),
      filter: sinon.stub().returnsThis(),
      forEach: sinon.stub().callsArgWith(0, overlayNode)
    };
  });

  afterEach(() => {
    __ResetDependency__('dispatchAction');
    __ResetDependency__('updateOverlayVisibility');
    __ResetDependency__('isBpmnType');
    __ResetDependency__('createEnterGatewayAnalysisModeAction');
    __ResetDependency__('createSetElementAction');
  });

  it('should set the end event', () => {
    const element = {id: 'element'};

    setEndEvent(element);

    expect(dispatchAction.calledWith(SET_ELEMENT)).to.eql(true);
    expect(createSetElementAction.calledWith('element', 'endEvent')).to.eql(true);
  });

  it('should set the gateway', () => {
    const element = {id: 'element'};

    setGateway(element);

    expect(dispatchAction.calledWith(SET_ELEMENT)).to.eql(true);
    expect(createSetElementAction.calledWith('element', 'gateway')).to.eql(true);
  });

  it('should unset the end event', () => {
    unsetEndEvent();

    expect(dispatchAction.calledWith(SET_ELEMENT)).to.eql(true);
    expect(createSetElementAction.calledWith(null, 'endEvent')).to.eql(true);
  });

  it('should unset the gateway', () => {
    unsetGateway();

    expect(dispatchAction.calledWith(SET_ELEMENT)).to.eql(true);
    expect(createSetElementAction.calledWith(null, 'gateway')).to.eql(true);
  });

  it('should unset the gateway and end event when leaving the gateway analysis mode', () => {
    leaveGatewayAnalysisMode();

    expect(dispatchAction.calledTwice).to.eql(true);
    expect(createSetElementAction.calledWith(null, 'endEvent')).to.eql(true);
    expect(createSetElementAction.calledWith(null, 'gateway')).to.eql(true);
  });

  describe('hover overlays', () => {
    it('should add overlays on the elements', () => {
      addBranchOverlay(viewer, heatmapData);

      expect(viewer.add.calledWith('a1')).to.eql(true);
    });

    it('should add an overlay with the piCount, element value and percentage as text content', () => {
      addBranchOverlay(viewer, heatmapData);

      const node = viewer.add.getCall(0).args[2].html;

      expect(node.textContent).to.contain('10');
      expect(node.textContent).to.contain('5');
      expect(node.textContent).to.contain('50%');
    });

    it('should add the overlay with the correct type', () => {
      addBranchOverlay(viewer, heatmapData);

      const type = viewer.add.getCall(0).args[1];

      expect(type).to.eql(BRANCH_OVERLAY);
    });

    it('should update the overlay visibility on hover', () => {
      hoverElement(viewer, 'a1');

      expect(updateOverlayVisibility.calledWith(viewer, 'a1', BRANCH_OVERLAY)).to.eql(true);
    });

    it('should show a selected overlay and keep it open', () => {
      showSelectedOverlay(viewer, 'a1');

      expect(overlayNode.keepOpen).to.be.true;
      expect(overlayNode.html.style.display).to.eql('block');
    });
  });

  describe('isValidElement', () => {
    let gatewayTwoOutgoing;
    let gatewayOneOutgoing;
    let endEvent;

    beforeEach(() => {
      gatewayTwoOutgoing = {
        businessObject: {
          outgoing: [1, 2]
        }
      };
      gatewayOneOutgoing = {
        businessObject: {
          outgoing: [1]
        }
      };
      endEvent = {};

      isBpmnType.returns(false);
      isBpmnType.withArgs(gatewayOneOutgoing, 'Gateway').returns(true);
      isBpmnType.withArgs(gatewayTwoOutgoing, 'Gateway').returns(true);
      isBpmnType.withArgs(endEvent, 'EndEvent').returns(true);
    });

    it('should be valid for every endEvent', () => {
      expect(isValidElement(endEvent, 'EndEvent')).to.be.true;
    });

    it('should be valid for gateways with multiple outgoing sequence flows', () => {
      expect(isValidElement(gatewayTwoOutgoing, 'Gateway')).to.be.true;
    });

    it('should not be valid for gateways with only one outgoing sequence flow', () => {
      expect(isValidElement(gatewayOneOutgoing, 'Gateway')).to.be.false;
    });
  });
});
