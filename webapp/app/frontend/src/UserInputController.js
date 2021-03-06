import Immutable from 'immutable'
import React from 'react'
import {shouldComponentUpdate} from 'react-immutable-render-mixin'
const vis = require('vis')
const uuid = require('uuid')

import {Card, CardHeader, CardText} from 'material-ui/Card'
import DatePicker from 'material-ui/DatePicker'
import RaisedButton from 'material-ui/RaisedButton'
import TextField from 'material-ui/TextField'
import Toggle from 'material-ui/Toggle'


class UserInputController extends React.Component {
    static propTypes = {
        onSubmitQuery: React.PropTypes.func.isRequired,
        onUpdateQueryEndDate: React.PropTypes.func.isRequired,
        onUpdateQueryStartDate: React.PropTypes.func.isRequired,
        onUpdateQueryWord: React.PropTypes.func.isRequired,
        queryEndDate: React.PropTypes.string.isRequired,
        queryStartDate: React.PropTypes.string.isRequired,
        queryWord: React.PropTypes.string.isRequired
    }

    state = {
        currentDate: new Date(),
        defaultStartDate: new Date("2014-12-01"),
        defaultEndDate: new Date("2014-12-31"),
        expanded: false
    }

    shouldComponentUpdate = shouldComponentUpdate

    render() {
        const queryStartDate = this.props.queryStartDate === '' ?
            this.state.defaultStartDate : new Date(this.props.queryStartDate)
        const queryEndDate = this.props.queryEndDate === '' ?
            this.state.defaultEndDate : new Date(this.props.queryEndDate)

        return (
            <Card
                expanded={this.state.expanded}
                onExpandChange={this.handleExpandChange}
            >
                <CardHeader
                    actAsExpander={false}
                    showExpandableButton={false}
                    title="How can I help you today?"
                />
                <CardText>
                    <TextField
                        hintText="Pizza"
                        floatingLabelText="Enter search keyword"
                        onChange={this.onChangeQueryWord}
                        value={this.props.queryWord}
                    />
                    <RaisedButton
                        label="Submit"
                        onClick={this.onSubmitQuery}
                        primary={true}
                    />
                    <br/>
                    <Toggle
                        label="Set date range"
                        labelPosition="right"
                        onToggle={this.handleExpandChange}
                        toggled={this.state.expanded}
                    />
                </CardText>
                <CardText
                    expandable={true}
                    style={{paddingTop: 0}}
                >
                    <DatePicker
                        autoOk={true}
                        disableYearSelection={false}
                        floatingLabelText="Start date"
                        maxDate={this.state.currentDate}
                        mode="landscape"
                        onChange={this.onChangeStartDate}
                        value={queryStartDate}
                    />
                    <DatePicker
                        autoOk={true}
                        disableYearSelection={false}
                        floatingLabelText="End Date"
                        maxDate={this.state.currentDate}
                        mode="landscape"
                        onChange={this.onChangeEndDate}
                        value={queryEndDate}
                    />
                </CardText>
            </Card>
        )
    }

    handleExpandChange = () => {
        const expanded = !this.state.expanded
        this.setState({expanded})

        const startDateString = expanded ?
            this.state.defaultStartDate.toISOString() : ''
        const endDateString = expanded ?
            this.state.defaultEndDate.toISOString() : ''

        this.props.onUpdateQueryStartDate(startDateString)
        this.props.onUpdateQueryEndDate(endDateString)
    }

    onSubmitQuery = () => {
        this.props.onSubmitQuery()
    }

    onChangeQueryWord = (event) => {
        this.props.onUpdateQueryWord(event.target.value)
    }

    onChangeStartDate = (event, date) => {
        this.props.onUpdateQueryStartDate(
            date.toISOString()
        )
    }

    onChangeEndDate = (event, date) => {
        this.props.onUpdateQueryEndDate(
            date.toISOString()
        )
    }
}

export default UserInputController
